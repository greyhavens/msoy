//
// $Id$

package com.threerings.msoy.server;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.util.Comparators;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;

import com.threerings.msoy.data.MemberLocation;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.peer.data.HostedPlace;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.world.data.MsoySceneModel;

/**
 * Contains a snapshot of all of the popular places in the Whirled.
 */
public class PopularPlacesSnapshot
{
    /** We keep track of only this many top scenes and games. */
    public static final int MAX_TRACKED_PLACES = 50;

    /** Contains id, name and population of a place. */
    public static class Place implements Comparable<Place> {
        /** The place id. */
        public int placeId;

        /** The place's name. */
        public String name;

        /** The place's population (as of the last snapshot calculation). */
        public int population;

        // from interface Comparable<Place>
        public int compareTo (Place other) {
            // higher population sorts first, then sort alphabetically
            int rv = Comparators.compare(other.population, population);
            return (rv == 0) ? name.compareTo(other.name) : rv;
        }
    }

    /**
     * Iterates over all the games, lobbies and the scenes in the world, finds out the N most
     * populated ones and sorts all scenes by owner, caching the values.
     */
    @EventThread
    public static PopularPlacesSnapshot takeSnapshot (
        PresentsDObjectMgr omgr, MsoyPeerManager peerMan)
    {
        omgr.requireEventThread();
        return new PopularPlacesSnapshot(peerMan);
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
     * in descending order of population. <em>Note:</em> the {@link Place#name} field for these
     * records is not valid and the {@link Place#placeId} field is the Whirled's id.
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

    protected PopularPlacesSnapshot (MsoyPeerManager peerMan)
    {
        // any member on any world server can be playing any game on any game server, so we first
        // need to make a first pass to collect metadata on all hosted games
        final IntMap<HostedPlace> hostedGames = IntMaps.newHashIntMap();
        peerMan.applyToNodes(new PeerManager.Operation() {
            public void apply (NodeObject nodeobj) {
                MsoyNodeObject mnobj = (MsoyNodeObject)nodeobj;
                for (HostedPlace game : mnobj.hostedGames) {
                    hostedGames.put(game.placeId, game);
                }
            }
        });

        // now we can count up the population in all scenes and games
        peerMan.applyToNodes(new PeerManager.Operation() {
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
                        HostedPlace game = hostedGames.get(memloc.gameId);
                        if (game != null) {
                            // map games by game id
                            increment(_games, _glist, game.placeId, game);
                        }
                    }
                }
            }

            protected void increment (IntMap<Place> places, List<Place> plist,
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

        // sort and prune our top places list
        sortAndPrune(_whlist);
        sortAndPrune(_sclist);
        sortAndPrune(_glist);
    }

    protected static void sortAndPrune (List<Place> list)
    {
        Collections.sort(list);
        while (list.size() > MAX_TRACKED_PLACES) {
            list.remove(list.size()-1);
        }
    }

    /** The total number of people in the Whirled. */
    protected int _totalPopulation;

    /** A mapping of all resolved whirleds in the whole wide Whirled. */
    protected IntMap<Place> _whirleds = IntMaps.newHashIntMap();

    /** A mapping of all resolved scenes in the whole wide Whirled. */
    protected IntMap<Place> _scenes = IntMaps.newHashIntMap();

    /** A mapping of all resolved games in the whole wide Whirled. */
    protected IntMap<Place> _games = IntMaps.newHashIntMap();

    /** The most popular whirleds, sorted. */
    protected List<Place> _whlist = Lists.newArrayList();

    /** The most popular scenes, sorted. */
    protected List<Place> _sclist = Lists.newArrayList();

    /** The most popular games, sorted. */
    protected List<Place> _glist = Lists.newArrayList();
}
