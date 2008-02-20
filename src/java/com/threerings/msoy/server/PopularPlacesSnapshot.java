//
// $Id$

package com.threerings.msoy.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.SortableArrayList;

import com.threerings.msoy.data.MemberLocation;

import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.peer.data.HostedPlace;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.data.MsoyNodeObject;

import com.threerings.msoy.web.data.PlaceCard;
import com.threerings.msoy.world.data.MsoySceneModel;

/**
 * Contains a snapshot of all of the popular places in the Whirled.
 */
public class PopularPlacesSnapshot
{
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
    public Iterable<PlaceCard> getTopScenes ()
    {
        return _sclist;
    }

    /**
     * Returns a list of the {@link #MAX_TRACKED_PLACES} most popular whirleds in the world, sorted
     * in descending order of population. <em>Note:</em> the {@link PlaceCard#name} field for these
     * records is not valid and the {@link PlaceCard#placeId} field is the Whirled's id.
     */
    public Iterable<PlaceCard> getTopWhirleds ()
    {
        return _whlist;
    }

    /**
     * Returns a list of the {@link #MAX_TRACKED_PLACES} most popular games in the world, sorted in
     * descending order of population.
     */
    public Iterable<PlaceCard> getTopGames ()
    {
        return _glist;
    }

    /**
     * Returns the place summary information for the specified whirled.
     */
    public PlaceCard getWhirled (int whirledId)
    {
        return _whirleds.get(whirledId);
    }

    /**
     * Returns the place summary information for the specified scene.
     */
    public PlaceCard getScene (int sceneId)
    {
        return _scenes.get(sceneId);
    }

    /**
     * Returns the place summary information for the specified game.
     */
    public PlaceCard getGame (int gameId)
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

            protected void increment (IntMap<PlaceCard> places, List<PlaceCard> plist,
                                      int placeId, HostedPlace hp) {
                PlaceCard place = places.get(placeId);
                if (place == null) {
                    place = new PlaceCard();
                    place.placeId = placeId;
                    place.name = hp.name;
                    places.put(placeId, place);
                    plist.add(place);
                }
                place.population++;
            }
        });

        // sort and prune our top places list
        sortAndPrune(_whlist);
        sortAndPrune(_sclist);
        sortAndPrune(_glist);
    }

    protected static void sortAndPrune (SortableArrayList<PlaceCard> list)
    {
        list.sort(Collections.reverseOrder(PLACE_COMP));
        while (list.size() > MAX_TRACKED_PLACES) {
            list.remove(list.size()-1);
        }
    }

    /** The total number of people in the Whirled. */
    protected int _totalPopulation;

    /** A mapping of all resolved whirleds in the whole wide Whirled. */
    protected IntMap<PlaceCard> _whirleds = IntMaps.newHashIntMap();

    /** A mapping of all resolved scenes in the whole wide Whirled. */
    protected IntMap<PlaceCard> _scenes = IntMaps.newHashIntMap();

    /** A mapping of all resolved games in the whole wide Whirled. */
    protected IntMap<PlaceCard> _games = IntMaps.newHashIntMap();

    /** The most popular whirleds, sorted. */
    protected SortableArrayList<PlaceCard> _whlist = new SortableArrayList<PlaceCard>();

    /** The most popular scenes, sorted. */
    protected SortableArrayList<PlaceCard> _sclist = new SortableArrayList<PlaceCard>();

    /** The most popular games, sorted. */
    protected SortableArrayList<PlaceCard> _glist = new SortableArrayList<PlaceCard>();

    /** Used to sort places. */
    protected static final Comparator<PlaceCard> PLACE_COMP = new Comparator<PlaceCard>() {
        public int compare (PlaceCard one, PlaceCard two) {
            int rv = (one.population - two.population);
            return (rv == 0) ? one.name.compareTo(two.name) : rv;
        }
    };
}
