package com.threerings.msoy.client {

import mx.core.ClassFactory;
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
        mctx :MsoyContext, field :String, renderer :Class,
        sortFn :Function = null, filterFn :Function = null)
    {
        _field = field;

        styleName = "friendList"; // TODO: "peerList";
        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.ON;
        percentWidth = 100;
        percentHeight = 100;
        selectable = false;
        variableRowHeight = true;

        var cf :ClassFactory = new ClassFactory(renderer);
        cf.properties =  { mctx: mctx };
        itemRenderer = cf;

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
            _list.disableAutoUpdate();
            try {
                removePlayer(PlayerEntry(event.getOldEntry()));
                addPlayer(PlayerEntry(event.getEntry()));
            } finally {
                _list.enableAutoUpdate();
            }
            _list.refresh();
        }
    }

    // from SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        if (event.getName() == _field) {
            removePlayer(PlayerEntry(event.getOldEntry()));
        }
    }

    protected function addPlayer (player :PlayerEntry) :void
    {
        _list.addItem(player);
        _list.refresh();
    }

    protected function removePlayer (player :PlayerEntry) :void
    {
        const key :Object = player.getKey();
        for (var ii :int = 0; ii < _list.length; ii++) {
            if (Util.equals(key, PlayerEntry(_list.getItemAt(ii)).getKey())) {
                _list.removeItemAt(ii);
                _list.refresh();
                return;
            }
        }
    }

    protected var _list :ArrayCollection;

    protected var _field :String;
}
}
