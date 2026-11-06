package lab3.model;

import java.util.List;

public class LocationDetails {

    private final Location location;
    private final Weather weather;
    private final List<Place> places;

    public LocationDetails(Location location, Weather weather, List<Place> places) {
        this.location = location;
        this.weather = weather;
        this.places = places;
    }

    public Location getLocation() {
        return location;
    }

    public Weather getWeather() {
        return weather;
    }

    public List<Place> getPlaces() {
        return places;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append("  ").append(location).append("\n");
        sb.append("========================================\n\n");

        sb.append("Погода: ").append(weather).append("\n\n");

        if (places.isEmpty()) {
            sb.append("Интересных мест поблизости не найдено.\n");
        } else {
            sb.append("Интересные места поблизости:\n");
            for (int i = 0; i < places.size(); i++) {
                sb.append(String.format("\n %d. %s\n", i + 1, places.get(i)));
            }
        }

        sb.append("\n========================================\n");
        return sb.toString();
    }
}
