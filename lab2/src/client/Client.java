package client;

import protocol.Protocol;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class Client {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Использование: java client.Client <путь_к_файлу> <хост> <порт>");
            System.exit(1);
        }

        String filePath = args[0];
        String host = args[1];
        int port;
        try {
            port = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println("Некорректный порт: " + args[2]);
            System.exit(1);
            return;
        }

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("Файл не найден: " + filePath);
            System.exit(1);
            return;
        }

        String fileName = file.getName();
        long fileSize = file.length();
        byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);

        if (fileNameBytes.length > Protocol.MAX_FILENAME_BYTES) {
            System.err.println("Имя файла слишком длинное (> " + Protocol.MAX_FILENAME_BYTES + " байт в UTF-8)");
            System.exit(1);
        }

        System.out.println("Отправка файла '" + fileName + "' (" + fileSize + " байт) на " + host + ":" + port);

        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream in = new DataInputStream(socket.getInputStream());
             FileInputStream fis = new FileInputStream(file)) {

            // ——— Отправляем заголовок ———
            out.writeInt(fileNameBytes.length);   //  (4 байта)
            out.write(fileNameBytes);
            out.writeLong(fileSize);               // (8 байт)

            // ——— Отправляем содержимое файла ———
            byte[] buffer = new byte[Protocol.BUFFER_SIZE];
            long totalSent = 0;
            int read;
            while ((read = fis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                totalSent += read;
            }
            out.flush();

            System.out.println("Отправлено " + totalSent + " байт, ожидание подтверждения...");

            // ——— Читаем ответ сервера ———
            byte status = in.readByte();
            if (status == Protocol.STATUS_SUCCESS) {
                System.out.println("Файл успешно передан!");
            } else {
                System.out.println("Ошибка: сервер сообщил о неудачной передаче файла.");
            }

        } catch (IOException e) {
            System.err.println("Ошибка передачи: " + e.getMessage());
        }
    }
}


//java -cp out server.Server 12345  
//java -cp out client.Client testfile.txt localhost 12345