package es.moiseslodeiro.ibri;

/**
 * Created by moises on 19/07/16.
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
