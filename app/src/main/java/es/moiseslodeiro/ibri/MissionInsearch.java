package es.moiseslodeiro.ibri;

/**
 * The MissionInsearch is a representation of the persons that are in search
 * @author Moisés Lodeiro Santiago
 */
public class MissionInsearch {

    private String PhysicalWeb = "";

    /**
     * Gets physical web.
     *
     * @return the physical web
     */
    public String getPhysicalWeb() {
        return PhysicalWeb;
    }

    /**
     * Sets physical web.
     *
     * @param physicalWeb the physical web
     */
    public void setPhysicalWeb(String physicalWeb) {
        PhysicalWeb = physicalWeb;
    }

    @Override
    public String toString() {
        return "MissionInsearch {"+this.getPhysicalWeb()+"}";
    }

}
