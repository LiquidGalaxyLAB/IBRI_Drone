package es.moiseslodeiro.ibri;

/**
 * The DataDron class is just a representation of an object that represents the data that represents
 * a drone.
 *
 * @author Moisés Lodeiro Santiago
 */

public class DataDron {

    /**
     * The Drone id.
     */
    public int droneId = 0;
    /**
     * The Mission id.
     */
    public int missionId = 0;
    /**
     * The Latitude.
     */
    public double latitude = 0.0;
    /**
     * The Longitude.
     */
    public double longitude = 0.0;
    /**
     * The Near latitude
     */
    public double nearLat = 0.0;
    /**
     * The Near longitude
     */
    public double nearLng = 0.0;
    /**
     * The Nearpoint (based on the point list)
     */
    public int nearpoint = -1;
    /**
     * The Photo on base64 format
     */
    public String photo = "";
    /**
     * The Beacon (Eddystone URL)
     */
    public String beacon = "";

}
