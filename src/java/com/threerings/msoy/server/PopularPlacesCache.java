//
// $Id$

package com.threerings.msoy.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.samskivert.util.CountHashMap;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.msoy.data.PopularPlace;
import com.threerings.msoy.data.PopularPlace.PopularGamePlace;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.server.LobbyManager;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.world.data.WorldMemberInfo;
import com.threerings.parlor.data.Table;

public class PopularPlacesCache
{
    /**
     * Look up and return the population of a given {@link PopularPlace}.
     */
    public int getPopulation (PopularPlace place)
    {
        return _population.getCount(place);
    }

    /**
     * Return a list of the most populous places in the whirled, sorted by population.
     */
    public Iterable<PopularPlace> getTopPlaces ()
    {
        return _topPlaces;
    }

    /**
     * Return the total population count in the whirled.
     */
    public int getPopulationCount ()
    {
        return _totalPopulation;
    }

    /**
     * Iterates over all the lobbies and the scenes in the world at the moment, finds out the
     * n most populated ones and sorts all scenes by owner, caching the values.
     *
     * This must be called on the dobj thread.
     */
    protected PopularPlacesCache ()
    {
        _population = new CountHashMap<PopularPlace>();
        _topPlaces = new LinkedList<PopularPlace>();
        _totalPopulation = 0;
        Iterator<?> i = MsoyServer.plreg.enumeratePlaceManagers();
        while (i.hasNext()) {
            PlaceManager plMgr = (PlaceManager) i.next();
            PlaceObject plObj = plMgr.getPlaceObject();

            int count = 0;
            for (OccupantInfo info : plObj.occupantInfo) {
                if (info instanceof WorldMemberInfo) {
                    count ++;
                }
            }
            // don't track places without members in them at all
            if (count == 0) {
                continue;
            }

            PopularPlace place = PopularPlace.getPopularPlace(plMgr);
            _population.incrementCount(place, count);
            _topPlaces.add(place);
            _totalPopulation += count;
        }

        // let's iterate over the lobbies too, which are no longer places
        i = MsoyServer.lobbyReg.enumerateLobbyManagers();
        while (i.hasNext()) {
            LobbyObject lObj = ((LobbyManager) i.next()).getLobbyObject();

            // then add up the population count for each table being formed in this lobby
            int count = 0;
            for (Table table : lObj.tables) {
                count += table.getOccupiedCount();
            }

            // we skip empty lobbies, obviously
            if (count == 0) {
                continue;
            }

            // otherwise we're most likely dealing with a game that's already been registered
            Game game = lObj.game;
            PopularGamePlace place = new PopularGamePlace(game.name, game.getPrototypeId());
            _population.incrementCount(place, count);
            _topPlaces.add(place);

            // we don't increase total population: these are people we've already counted once
        }

        /*
         * TODO: this is currently O(N log N) in the number of rooms; if that is unrealistic in
         * the long run, we can easily make it O(N) by just bubbling into the top 20 (whatever)
         * rooms on the fly, as we enumerate the scene managers.
         * */
        Collections.sort(_topPlaces, new Comparator<PopularPlace>() {
            public int compare (PopularPlace o1, PopularPlace o2) {
                int p1 = _population.getCount(o1);
                int p2 = _population.getCount(o2);
                return (p1 > p2) ? -1 : ((p1 == p2) ? 0 : 1);
            }
        });
    }

    /** A mapping of popular places to population counts. */
    protected CountHashMap<PopularPlace> _population;

    /** A list of every place (lobby or scene) in the world, sorted by population. */
    protected List<PopularPlace> _topPlaces;

    /** The total number of people in the whirled. */
    protected int _totalPopulation;
}
