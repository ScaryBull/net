package lab3.model;

public class Place {

    private final int pageId;
    private final String title;
    private final double distance;
    private final String description;

    public Place(int pageId, String title, double distance, String description) {
        this.pageId = pageId;
        this.title = title;
        this.distance = distance;
        this.description = description;
    }

    public int getPageId() {
        return pageId;
    }

    public String getTitle() {
        return title;
    }

    public double getDistance() {
        return distance;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        String shortDesc = description;
        if (shortDesc != null && shortDesc.length() > 300) {
            shortDesc = shortDesc.substring(0, 300) + "...";
        }
        return String.format("%s (%.0f м)\n   %s", title, distance, shortDesc);
    }
}
