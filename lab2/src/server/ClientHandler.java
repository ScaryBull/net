package server;

import protocol.Protocol;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;


public class ClientHandler implements Runnable {

    private final Socket socket;
    private final SpeedTracker speedTracker;
    private final String clientId;
    private final Path uploadsDir;

    public ClientHandler(Socket socket, SpeedTracker speedTracker, Path uploadsDir) {
        this.socket = socket;
        this.speedTracker = speedTracker;
        this.clientId = socket.getRemoteSocketAddress().toString();
        this.uploadsDir = uploadsDir;
    }

    @Override
    public void run() {
        speedTracker.registerClient(clientId);
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            // ——— Читаем заголовок ———
            int fileNameLength = in.readInt();
            if (fileNameLength <= 0 || fileNameLength > Protocol.MAX_FILENAME_BYTES) {
                throw new IOException("Некорректная длина имени файла: " + fileNameLength);
            }

            byte[] fileNameBytes = new byte[fileNameLength];
            in.readFully(fileNameBytes);
            String fileName = new String(fileNameBytes, StandardCharsets.UTF_8);

            long fileSize = in.readLong();
            if (fileSize < 0) {
                throw new IOException("Некорректный размер файла: " + fileSize);
            }

            String safeName = sanitizeFileName(fileName);

            // Проверяем, что итоговый путь внутри uploads/
            Path targetPath = uploadsDir.resolve(safeName).normalize();
            if (!targetPath.startsWith(uploadsDir.normalize())) {
                throw new IOException("Попытка записи за пределы uploads/: " + fileName);
            }

            System.out.println("Приём файла '" + safeName + "' (" + fileSize + " байт) от " + clientId);

            // ——— Принимаем данные файла ———
            long totalReceived = 0;
            byte[] buffer = new byte[Protocol.BUFFER_SIZE];

            try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
                while (totalReceived < fileSize) {
                    int toRead = (int) Math.min(buffer.length, fileSize - totalReceived);
                    int read = in.read(buffer, 0, toRead);
                    if (read == -1) {
                        break;
                    }

                    fos.write(buffer, 0, read);
                    totalReceived += read;
                    speedTracker.addBytes(clientId, read);
                }
            }

            // ——— Проверяем размер и отправляем ответ ———
            boolean success = (totalReceived == fileSize);
            out.writeByte(success ? Protocol.STATUS_SUCCESS : Protocol.STATUS_FAILURE);
            out.flush();

            if (success) {
                System.out.println("Файл '" + safeName + "' от " + clientId + " — успешно сохранён");
            } else {
                System.out.println("ОШИБКА: файл '" + safeName + "' от " + clientId
                        + " — получено " + totalReceived + " из " + fileSize + " байт");
            }

        } catch (Exception e) {
            System.err.println("Ошибка при обработке клиента " + clientId + ": " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) { }
            speedTracker.unregisterClient(clientId);
        }
    }

    //убирает любые компоненты пути, заменяет недопустимые символы
    private String sanitizeFileName(String rawName) {
        // Убираем слэши — берём только последний компонент
        String name = rawName;
        int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }

        name = name.replace("..", "_");
        name = name.replaceAll("[<>:\"|?*\\x00-\\x1f]", "_");
        if (name.isEmpty()) {
            name = "unnamed_file";
        }
        
        return name;
    }
}
