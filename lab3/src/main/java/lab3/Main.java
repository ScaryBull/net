package lab3;

import lab3.model.Location;
import lab3.model.LocationDetails;
import lab3.service.LocationService;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) {
        // Чтение API-ключей из переменных окружения
        String graphHopperKey = System.getenv("GRAPHHOPPER_API_KEY");
        String openWeatherKey = System.getenv("OPENWEATHER_API_KEY");

        if (graphHopperKey == null || graphHopperKey.isEmpty()) {
            System.err.println("Ошибка: не установлена переменная окружения GRAPHHOPPER_API_KEY");
            System.err.println("Получите ключ: https://www.graphhopper.com/dashboard/#/apikeys");
            System.exit(1);
        }

        if (openWeatherKey == null || openWeatherKey.isEmpty()) {
            System.err.println("Ошибка: не установлена переменная окружения OPENWEATHER_API_KEY");
            System.err.println("Получите ключ: https://home.openweathermap.org/api_keys");
            System.exit(1);
        }

        LocationService service = new LocationService(graphHopperKey, openWeatherKey);
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Поиск мест ===\n");

        while (true) {
            System.out.print("Введите название места (или 'exit' для выхода): ");
            String query = scanner.nextLine().trim();
            if (query.isEmpty()) {
                continue;
            }

            if ("exit".equalsIgnoreCase(query)) {
                System.out.println("До свидания!");
                break;
            }

            // Поиск локаций
            System.out.println("\nИщем локации...");
            CompletableFuture<List<Location>> searchFuture = service.searchLocations(query);
            List<Location> locations;
            try {
                locations = searchFuture.join();
            } catch (Exception e) {
                System.err.println("Ошибка при поиске: " + e.getMessage());
                continue;
            }

            if (locations.isEmpty()) {
                System.out.println("Ничего не найдено. Попробуйте другой запрос.\n");
                continue;
            }

            // Показываем найденные локации
            System.out.println("\nНайденные локации:");
            for (int i = 0; i < locations.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, locations.get(i));
            }

            // Пользователь выбирает локацию
            System.out.print("\nВыберите номер локации (1-" + locations.size() + "): ");
            String choiceStr = scanner.nextLine().trim();
            int choice;
            try {
                choice = Integer.parseInt(choiceStr);
            } catch (NumberFormatException e) {
                System.err.println("Неверный ввод.\n");
                continue;
            }

            if (choice < 1 || choice > locations.size()) {
                System.err.println("Номер должен быть от 1 до " + locations.size() + ".\n");
                continue;
            }

            Location selected = locations.get(choice - 1);

            // Получение деталей
            System.out.println("\nЗагружаем погоду и интересные места...");
            CompletableFuture<LocationDetails> detailsFuture = service.getLocationDetails(selected);
            LocationDetails details;
            try {
                details = detailsFuture.join();
            } catch (Exception e) {
                System.err.println("Ошибка при загрузке деталей: " + e.getMessage());
                continue;
            }

            // Вывод результата
            System.out.println(details);
        }

        scanner.close();
    }
}
