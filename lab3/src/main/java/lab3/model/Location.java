package lab3.model;


public class Location {

    private final String name;
    private final String country;
    private final String state;
    private final String city;
    private final double lat;
    private final double lon;

    public Location(String name, String country, String state, String city, double lat, double lon) {
        this.name = name;
        this.country = country;
        this.state = state;
        this.city = city;
        this.lat = lat;
        this.lon = lon;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public String getState() {
        return state;
    }

    public String getCity() {
        return city;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (name != null) sb.append(name);
        if (city != null && !city.isEmpty() && !city.equals(name)) sb.append(", ").append(city);
        if (state != null && !state.isEmpty()) sb.append(", ").append(state);
        if (country != null && !country.isEmpty()) sb.append(", ").append(country);
        sb.append(String.format(" (%.4f, %.4f)", lat, lon));
        return sb.toString();
    }
}
