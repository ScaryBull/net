package lab3.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lab3.model.Location;
import lab3.model.LocationDetails;
import lab3.model.Place;
import lab3.model.Weather;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class LocationService {

    private final HttpClient httpClient;
    private final String graphHopperApiKey;
    private final String openWeatherApiKey;

    public LocationService(String graphHopperApiKey, String openWeatherApiKey) {
        this.httpClient = HttpClient.newHttpClient();
        this.graphHopperApiKey = graphHopperApiKey;
        this.openWeatherApiKey = openWeatherApiKey;
    }

    // Асинхронный GET-запрос, возвращающий тело ответа как строку.
    private CompletableFuture<String> fetchJson(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "PlacesApp/1.0 (student@nsu.ru)")
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException(
                                "HTTP " + response.statusCode() + " for " + url + ": " + response.body());
                    }
                    return response.body();
                });
    }

    // Поиск локаций (шаг 2)
    public CompletableFuture<List<Location>> searchLocations(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format(
                "https://graphhopper.com/api/1/geocode?q=%s&locale=ru&limit=10&key=%s",
                encodedQuery, graphHopperApiKey);
        return fetchJson(url).thenApply(this::parseLocations);
    }

    private List<Location> parseLocations(String json) {
        List<Location> locations = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray hits = root.getAsJsonArray("hits");
        if (hits != null) {
            for (JsonElement hit : hits) {
                JsonObject obj = hit.getAsJsonObject();
                JsonObject point = obj.getAsJsonObject("point");
                locations.add(new Location(
                        getStringOrNull(obj, "name"),
                        getStringOrNull(obj, "country"),
                        getStringOrNull(obj, "state"),
                        getStringOrNull(obj, "city"),
                        point.get("lat").getAsDouble(),
                        point.get("lng").getAsDouble()
                ));
            }
        }
        return locations;
    }

    // Детали локации (шаги 4 и 5)
    public CompletableFuture<LocationDetails> getLocationDetails(Location location) {
        CompletableFuture<Weather> weatherFuture =
                fetchWeather(location.getLat(), location.getLon());
        CompletableFuture<List<Place>> placesFuture =
                fetchNearbyPlaces(location.getLat(), location.getLon());
        return weatherFuture.thenCombine(placesFuture,
                (weather, places) -> new LocationDetails(location, weather, places));
    }

    // Погода (OpenWeatherMap)
    private CompletableFuture<Weather> fetchWeather(double lat, double lon) {
        String url = String.format(Locale.US,
                "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric&lang=ru",
                lat, lon, openWeatherApiKey);
        return fetchJson(url).thenApply(this::parseWeather);
    }

    private Weather parseWeather(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray weatherArr = root.getAsJsonArray("weather");
        String description = weatherArr.get(0).getAsJsonObject().get("description").getAsString();
        JsonObject main = root.getAsJsonObject("main");
        JsonObject wind = root.getAsJsonObject("wind");
        return new Weather(
                description,
                main.get("temp").getAsDouble(),
                main.get("feels_like").getAsDouble(),
                main.get("humidity").getAsInt(),
                wind.get("speed").getAsDouble()
        );
    }

    // Интересные места (Wikipedia)
    private CompletableFuture<List<Place>> fetchNearbyPlaces(double lat, double lon) {
        String url = String.format(Locale.US,
                "https://ru.wikipedia.org/w/api.php?action=query&list=geosearch&gscoord=%f%%7C%f&gsradius=10000&gslimit=10&format=json",
                lat, lon);

        return fetchJson(url).thenCompose(json -> {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray geosearch = root.getAsJsonObject("query").getAsJsonArray("geosearch");
            List<CompletableFuture<Place>> placeFutures = new ArrayList<>();
            for (JsonElement elem : geosearch) {
                JsonObject obj = elem.getAsJsonObject();
                int pageId = obj.get("pageid").getAsInt();
                String title = obj.get("title").getAsString();
                double dist = obj.get("dist").getAsDouble();
                CompletableFuture<Place> placeFuture = fetchPlaceDescription(pageId)
                        .thenApply(desc -> new Place(pageId, title, dist, desc));
                placeFutures.add(placeFuture);
            }

            return CompletableFuture.allOf(placeFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> placeFutures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList()));
        });
    }

    // Описание статьи Wikipedia по её pageId
    private CompletableFuture<String> fetchPlaceDescription(int pageId) {
        String url = String.format(
                "https://ru.wikipedia.org/w/api.php?action=query&prop=extracts&exintro=true&explaintext=true&pageids=%d&format=json",
                pageId);
        return fetchJson(url).thenApply(json -> {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject pages = root.getAsJsonObject("query").getAsJsonObject("pages");
            JsonObject page = pages.entrySet().iterator().next().getValue().getAsJsonObject();

            if (page.has("extract") && !page.get("extract").getAsString().isEmpty()) {
                return page.get("extract").getAsString();
            }
            return "Описание не найдено";
        });
    }

    private String getStringOrNull(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }
}
