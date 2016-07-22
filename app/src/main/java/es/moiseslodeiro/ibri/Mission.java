package es.moiseslodeiro.ibri;

import java.util.ArrayList;

/**
 * The Mission class is a representation to a Mission Object
 * @author Moisés Lodeiro Santiago
 */
public class Mission {

    /**
     * The Mission id.
     */
    public int missionId = 0;
    /**
     * The In search.
     */
    public ArrayList<MissionInsearch> inSearch = new ArrayList<MissionInsearch>();
    /**
     * The Positions.
     */
    public ArrayList<MissionPosition> positions = new ArrayList<MissionPosition>();

}
