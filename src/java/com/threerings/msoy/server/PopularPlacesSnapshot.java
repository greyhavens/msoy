//
// $Id$

package com.threerings.msoy.server;

import java.util.List;

import com.samskivert.util.ComparableArrayList;
import com.samskivert.util.HashIntMap;

import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.peer.data.HostedPlace;
import com.threerings.msoy.peer.data.MemberLocation;
import com.threerings.msoy.peer.data.MsoyNodeObject;

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

    /** We keep track of only this many top scenes game games. */
    public static final int MAX_TRACKED_PLACES = 50;

    /**
     * Returns a list of the {@link #MAX_TRACKED_PLACES} most popular scenes in the world, sorted
     * in descending order of population.
     */
    public Iterable<Place> getTopScenes ()
    {
        return _sclist;
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

    /**
     * Iterates over all the games, lobbies and the scenes in the world, finds out the N most
     * populated ones and sorts all scenes by owner, caching the values.
     *
     * This must be called on the dobj thread.
     */
    protected PopularPlacesSnapshot ()
    {
        // count up the population in all scenes and games
        MsoyServer.peerMan.applyToNodes(new PeerManager.Operation() {
            public void apply (NodeObject nodeobj) {
                MsoyNodeObject mnobj = (MsoyNodeObject)nodeobj;
                for (MemberLocation memloc : mnobj.memberLocs) {
                    _totalPopulation++;
                    if (memloc.type == MemberLocation.SCENE) {
                        increment(_scenes, _sclist, mnobj.hostedScenes.get(memloc.locationId));
                    } else if (memloc.type == MemberLocation.GAME) {
                        increment(_games, _glist, mnobj.hostedGames.get(memloc.locationId));
                    }
                    // TODO: handle players also in game lobbies
                }
            }

            protected void increment (HashIntMap<Place> places, List<Place> plist, HostedPlace hp) {
                if (hp == null) {
                    return;
                }
                Place place = places.get(hp.placeId);
                if (place == null) {
                    place = new Place();
                    place.placeId = hp.placeId;
                    place.name = hp.name;
                    places.put(hp.placeId, place);
                    plist.add(place);
                }
                place.population++;
            }
        });

        // sort and prune our top places list
        _sclist.sort();
        while (_sclist.size() > MAX_TRACKED_PLACES) {
            _sclist.remove(_sclist.size()-1);
        }
        _glist.sort();
        while (_glist.size() > MAX_TRACKED_PLACES) {
            _glist.remove(_glist.size()-1);
        }

//         // now turn these into proper records
//         final ArrayList<Place> scenes = new ArrayList<Place>();
//         final ArrayList<Game> games = new ArrayList<Game>();

//         _population = new CountHashMap<PopularPlace>();
//         _topPlaces = new ArrayList<PopularPlace>();
//         _totalPopulation = 0;

//         Iterator<?> i = MsoyServer.plreg.enumeratePlaceManagers();
//         while (i.hasNext()) {
//             PlaceManager plMgr = (PlaceManager) i.next();
//             PlaceObject plObj = plMgr.getPlaceObject();

//             int count = 0;
//             for (OccupantInfo info : plObj.occupantInfo) {
//                 if (info instanceof WorldMemberInfo) {
//                     count ++;
//                 }
//             }
//             // don't track places without members in them at all
//             if (count == 0) {
//                 continue;
//             }

//             PopularPlace place = PopularPlace.getPopularPlace(plMgr);
//             _population.incrementCount(place, count);
//             _topPlaces.add(place);
//             _totalPopulation += count;
//         }

//         // let's iterate over the lobbies too, which are no longer places
//         i = MsoyServer.lobbyReg.enumerateLobbyManagers();
//         while (i.hasNext()) {
//             LobbyObject lObj = ((LobbyManager) i.next()).getLobbyObject();

//             // then add up the population count for each table being formed in this lobby
//             int count = 0;
//             for (Table table : lObj.tables) {
//                 count += table.getOccupiedCount();
//             }

//             // we skip empty lobbies, obviously
//             if (count == 0) {
//                 continue;
//             }

//             // otherwise we're most likely dealing with a game that's already been registered
//             Game game = lObj.game;
//             PopularGamePlace place = new PopularGamePlace(game.name, game.getPrototypeId());
//             _population.incrementCount(place, count);
//             _topPlaces.add(place);

//             // we don't increase total population: these are people we've already counted once
//         }

//         /*
//          * TODO: this is currently O(N log N) in the number of rooms; if that is unrealistic in
//          * the long run, we can easily make it O(N) by just bubbling into the top 20 (whatever)
//          * rooms on the fly, as we enumerate the scene managers.
//          * */
//         Collections.sort(_topPlaces, new Comparator<PopularPlace>() {
//             public int compare (PopularPlace o1, PopularPlace o2) {
//                 int p1 = _population.getCount(o1);
//                 int p2 = _population.getCount(o2);
//                 return (p1 > p2) ? -1 : ((p1 == p2) ? 0 : 1);
//             }
//         });
    }

    /** The total number of people in the whirled. */
    protected int _totalPopulation;

    /** A mapping of all resolved scenes in the whole wide Whirled. */
    protected HashIntMap<Place> _scenes = new HashIntMap<Place>();

    /** A mapping of all resolved games in the whole wide Whirled. */
    protected HashIntMap<Place> _games = new HashIntMap<Place>();

    /** The most popular scenes, sorted. */
    protected ComparableArrayList<Place> _sclist = new ComparableArrayList<Place>();

    /** The most popular games, sorted. */
    protected ComparableArrayList<Place> _glist = new ComparableArrayList<Place>();
}
