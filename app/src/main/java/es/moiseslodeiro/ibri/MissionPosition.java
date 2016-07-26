package es.moiseslodeiro.ibri;

/**
 * The MissionPosition is the representation of a Position model (GPS) using lat and lng parameters
 */
public class MissionPosition {

    private double lat = 0; // Latitude
    private double lng = 0; // Longitude

    /**
     * Gets lng.
     *
     * @return the lng
     */
    public double getLng() {
        return lng;
    }

    /**
     * Sets lng.
     *
     * @param lng the lng
     */
    public void setLng(double lng) {
        this.lng = lng;
    }

    /**
     * Gets lat.
     *
     * @return the lat
     */
    public double getLat() {
        return lat;
    }

    /**
     * Sets lat.
     *
     * @param lat the lat
     */
    public void setLat(double lat) {
        this.lat = lat;
    }

}
