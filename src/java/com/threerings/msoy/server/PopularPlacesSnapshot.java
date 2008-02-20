//
// $Id$

package com.threerings.msoy.server;

import java.util.List;

import com.samskivert.util.ComparableArrayList;
import com.samskivert.util.HashIntMap;

import com.threerings.msoy.data.MemberLocation;

import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.peer.data.HostedPlace;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.data.MsoyNodeObject;

import com.threerings.msoy.world.data.MsoySceneModel;

/**
 * Contains a snapshot of all of the popular places in the Whirled.
 */
public class PopularPlacesSnapshot
{
    /** Contains population info for a game or scene. */
    public static class Place implements Comparable<Place>
    {
        /** The unique integer identifier for this place. */
        public int placeId;

        /** The name of this place. */
        public String name;

        /** The number of people in this place. */
        public int population;

        // from interface Comparable<Place>
        public int compareTo (Place other) {
            int rv = (population - other.population);
            return (rv == 0) ? name.compareTo(other.name) : rv;
        }
    }

    /** We keep track of only this many top scenes and games. */
    public static final int MAX_TRACKED_PLACES = 50;

    /**
     * Iterates over all the games, lobbies and the scenes in the world, finds out the N most
     * populated ones and sorts all scenes by owner, caching the values.
     *
     * This must be called on the dobj thread.
     */
    public static PopularPlacesSnapshot takeSnapshot ()
    {
        MsoyServer.requireDObjThread();
        return new PopularPlacesSnapshot();
    }

    /**
     * Returns a list of the {@link #MAX_TRACKED_PLACES} most popular scenes in the world, sorted
     * in descending order of population.
     */
    public Iterable<Place> getTopScenes ()
    {
        return _sclist;
    }

    /**
     * Returns a list of the {@link #MAX_TRACKED_PLACES} most popular whirleds in the world, sorted
     * in descending order of population.
     */
    public Iterable<Place> getTopWhirleds ()
    {
        return _whlist;
    }

    /**
     * Returns a list of the {@link #MAX_TRACKED_PLACES} most popular games in the world, sorted in
     * descending order of population.
     */
    public Iterable<Place> getTopGames ()
    {
        return _glist;
    }

    /**
     * Returns the place summary information for the specified whirled.
     */
    public Place getWhirled (int whirledId)
    {
        return _whirleds.get(whirledId);
    }

    /**
     * Returns the place summary information for the specified scene.
     */
    public Place getScene (int sceneId)
    {
        return _scenes.get(sceneId);
    }

    /**
     * Returns the place summary information for the specified game.
     */
    public Place getGame (int gameId)
    {
        return _games.get(gameId);
    }

    /**
     * Return the total population count in the whirled.
     */
    public int getPopulationCount ()
    {
        return _totalPopulation;
    }

    protected PopularPlacesSnapshot ()
    {
        // count up the population in all scenes and games
        MsoyServer.peerMan.applyToNodes(new PeerManager.Operation() {
            public void apply (NodeObject nodeobj) {
                MsoyNodeObject mnobj = (MsoyNodeObject)nodeobj;
                for (MemberLocation memloc : mnobj.memberLocs) {
                    _totalPopulation++;
                    if (memloc.sceneId > 0) {
                        HostedRoom room = mnobj.hostedScenes.get(memloc.sceneId);
                        if (room == null || room.accessControl != MsoySceneModel.ACCESS_EVERYONE) {
                            // missing or private room, skip it
                        } else if (room.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
                            // map whirled rooms by whirled id
                            increment(_whirleds, _whlist, room.ownerId, room);
                        } else {
                            // map non-whirled rooms by scene id
                            increment(_scenes, _sclist, room.placeId, room);
                        }
                    }
                    if (memloc.gameId > 0) {
                        HostedPlace game = mnobj.hostedGames.get(memloc.gameId);
                        if (game != null) {
                            // map games by game id
                            increment(_games, _glist, game.placeId, game);
                        }
                    }
                }
            }

            protected void increment (HashIntMap<Place> places, List<Place> plist,
                                      int placeId, HostedPlace hp) {
                Place place = places.get(placeId);
                if (place == null) {
                    place = new Place();
                    place.placeId = placeId;
                    place.name = hp.name;
                    places.put(placeId, place);
                    plist.add(place);
                }
                place.population++;
            }
        });

        // sort and prune our top places list.  Use reverse sorting so the largest populations 
        // appear first.
        for (ComparableArrayList list : new ComparableArrayList[] { _whlist, _sclist, _glist }) {
            list.rsort();
            while (list.size() > MAX_TRACKED_PLACES) {
                list.remove(list.size()-1);
            }
        }
    }

    /** The total number of people in the Whirled. */
    protected int _totalPopulation;

    /** A mapping of all resolved whirleds in the whole wide Whirled. */
    protected HashIntMap<Place> _whirleds = new HashIntMap<Place>();

    /** A mapping of all resolved scenes in the whole wide Whirled. */
    protected HashIntMap<Place> _scenes = new HashIntMap<Place>();

    /** A mapping of all resolved games in the whole wide Whirled. */
    protected HashIntMap<Place> _games = new HashIntMap<Place>();

    /** The most popular whirleds, sorted. */
    protected ComparableArrayList<Place> _whlist = new ComparableArrayList<Place>();

    /** The most popular scenes, sorted. */
    protected ComparableArrayList<Place> _sclist = new ComparableArrayList<Place>();

    /** The most popular games, sorted. */
    protected ComparableArrayList<Place> _glist = new ComparableArrayList<Place>();
}
