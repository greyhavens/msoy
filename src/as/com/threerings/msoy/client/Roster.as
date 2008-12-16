package com.threerings.msoy.client {

import mx.core.IFactory;
import mx.core.ScrollPolicy;

import mx.collections.ArrayCollection;
import mx.collections.Sort;

import mx.controls.List;

import com.threerings.util.Util;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.msoy.data.all.PlayerEntry;

import com.threerings.msoy.client.MsoyContext;

/** A list of players, for use in popup friends and party lists. */
public class Roster extends List
    implements SetListener
{
    public function Roster (
        mctx :MsoyContext, field :String, renderer :IFactory,
        sortFn :Function = null, filterFn :Function = null)
    {
        _field = field;

        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.ON;
        percentWidth = 100;
        percentHeight = 100;
        selectable = false;
        variableRowHeight = true;

        itemRenderer = renderer;

        _list = new ArrayCollection();
        var sort :Sort = new Sort();
        sort.compareFunction = sortFn;
        _list.sort = sort;
        _list.filterFunction = filterFn;
        _list.refresh();
        dataProvider = _list;
    }

    public function init (players :Array) :void
    {
        _list.removeAll();
        for each (var player :PlayerEntry in players) {
            _list.addItem(player);
        }
        _list.refresh();
    }

    public function addPlayer (player :PlayerEntry) :void
    {
        _list.addItem(player);
        _list.refresh();
    }

    public function removePlayer (key :Object) :void
    {
        var idx :int = findKeyIndex(key);
        if (idx != -1) {
            _list.removeItemAt(idx);
            _list.refresh();
        }
    }

    /**
     * Update the specified player, replacing the entry with the same key.
     */
    public function updatePlayer (player :PlayerEntry) :void
    {
        trace("Must find: " + player);
        var idx :int = findKeyIndex(player.getKey());
        if (idx == -1) {
            throw new Error("Player not in list.");
        }
        _list.setItemAt(player, idx);
        _list.refresh();
    }

    // from SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == _field) {
            addPlayer(PlayerEntry(event.getEntry()));
        }
    }

    // from SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        if (event.getName() == _field) {
            updatePlayer(PlayerEntry(event.getEntry()));
        }
    }

    // from SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        if (event.getName() == _field) {
            removePlayer(event.getKey());
        }
    }

    protected function findKeyIndex (key :Object) :int
    {
        for (var ii :int = 0; ii < _list.length; ii++) {
            if (Util.equals(key, PlayerEntry(_list.getItemAt(ii)).getKey())) {
                return ii;
            }
        }
        return -1;
    }

    protected var _list :ArrayCollection;

    protected var _field :String;
}
}
