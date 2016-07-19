package utils;

/**
 * Created by moises on 19/07/16.
 */
public class MissionInsearch {

    private String PhysicalWeb = "";

    public String getPhysicalWeb() {
        return PhysicalWeb;
    }

    public void setPhysicalWeb(String physicalWeb) {
        PhysicalWeb = physicalWeb;
    }

    @Override
    public String toString() {
        return "MissionInsearch {"+this.getPhysicalWeb()+"}";
    }

}
