package com.threerings.msoy.client {

import mx.core.ClassFactory;
import mx.core.ScrollPolicy;

import mx.collections.ArrayCollection;
import mx.collections.Sort;

import mx.controls.List;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.PlayerEntry;

import com.threerings.msoy.client.MsoyContext;

/** Displays a pretty list of players, for use in popup friends and party lists. */
public class PlayerList extends List
    implements SetListener
{
    public function PlayerList (mctx :MsoyContext, field :String, renderer :Class)
    {
        _field = field;

        styleName = "friendList"; // TODO: "peerList";
        dataProvider = new ArrayCollection();
        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.ON;
        percentWidth = 100;
        percentHeight = 100;
        selectable = false;
        variableRowHeight = true;

        var cf :ClassFactory = new ClassFactory(renderer);;
        cf.properties =  { mctx: mctx };
        itemRenderer = cf;

        var sort :Sort = new Sort();
        sort.compareFunction = sortFunction;
        dataProvider.sort = sort;
        dataProvider.refresh();
    }

    public function init (players :Array) :void
    {
        dataProvider.removeAll();

        for each (var player :PlayerEntry in players) {
            addPlayer(player);
        }
    }

    // from SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == _field) {
            addPlayer(event.getEntry() as PlayerEntry);
        }
    }

    // from SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        if (event.getName() == _field) {
            var newEntry :PlayerEntry = event.getEntry() as PlayerEntry;
            var oldEntry :PlayerEntry = event.getOldEntry() as PlayerEntry;

            removePeer(oldEntry);
            addPlayer(newEntry);
        }
    }

    // from SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        if (event.getName() == _field) {
            removePeer(event.getOldEntry() as PlayerEntry);
        }
    }

    protected function addPlayer (player :PlayerEntry) :void
    {
        if ( ! dataProvider.contains(player)) {
            dataProvider.addItem(player);
        }
    }

    protected function removePeer (player :PlayerEntry) :void
    {
        var idx :int = dataProvider.getItemIndex(player);
        if (idx != -1) {
            dataProvider.removeItemAt(idx);
        }
    }

    protected function sortFunction (lhs :PlayerEntry, rhs :PlayerEntry, fields :Array = null) :int
    {
        return MemberName.BY_DISPLAY_NAME(lhs.name, rhs.name);
    }

    protected var _field :String;
}

}
