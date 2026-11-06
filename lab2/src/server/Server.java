package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Server {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Использование: java server.Server <порт>");
            System.exit(1);
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Некорректный порт: " + args[0]);
            System.exit(1);
            return;
        }

        // Создаём директорию uploads/, если её нет
        Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadsDir);
        } catch (IOException e) {
            System.err.println("Не удалось создать директорию uploads/: " + e.getMessage());
            System.exit(1);
        }

        SpeedTracker speedTracker = new SpeedTracker();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту " + port);
            System.out.println("Файлы сохраняются в: " + uploadsDir);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Новое соединение: " + clientSocket.getRemoteSocketAddress());
                Thread handler = new Thread(new ClientHandler(clientSocket, speedTracker, uploadsDir));
                handler.setDaemon(true);
                handler.start();
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        } finally {
            speedTracker.shutdown();
        }
    }
}
