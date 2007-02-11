package com.threerings.msoy.item.client {

import flash.events.Event;
import flash.events.EventDispatcher;

import mx.collections.ArrayCollection;

import com.threerings.util.Util;

import com.threerings.presents.client.ResultWrapper;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.web.Item;

/**
 * A special Collection that represents the items in the user's inventory
 * in a hierarchical fashion. This should be used as the source for the
 * Tree inside an InventoryPicker.
 */
public class InventoryCollectionView extends ArrayCollection
    implements AttributeChangeListener, SetListener
{
    /**
     * Construct a new InventoryCollectionView.
     */
    public function InventoryCollectionView (
        ctx :MsoyContext, soleType :int = Item.NOT_A_TYPE,
        showUsed :Boolean = false)
    {
        _ctx = ctx;
        _showUsed = showUsed;

        this.filterFunction = filterCategories;

        var allTypes :Array = Item.getTypes();
        for each (var type :int in allTypes) {
            var showList :ArrayCollection = new ArrayCollection();
            _showItems[type] = showList;
            addItem({
                label: Msgs.ITEM.get("t.items_" + Item.getTypeName(type)),
                itemType: type, children: showList
            });
        }

        // set the shown types
        if (soleType != Item.NOT_A_TYPE) {
            setShownTypes([ soleType ]);

        } else {
            setShownTypes(allTypes);
        }
    }

    /**
     * Start listening on the user object.
     */
    public function startup () :void
    {
        // start listening on the user object
        var memObj :MemberObject = _ctx.getClientObject();
        if (memObj == _memberObj) {
            return;
        }
        shutdown();
        _memberObj = memObj;
        _memberObj.addListener(this);

        if (_shownTypes.length == 1) {
            loadType(int(_shownTypes[0]));
        }
        regenerateAllShown();
    }

    /**
     * Are we currently active? That is, are we listening to the user object,
     * awaiting updates?
     */
    public function isActive () :Boolean
    {
        return (_memberObj != null);
    }

    /**
     * Dispose of this view: stop listening for updates to the user's
     * inventory.
     */
    public function shutdown () :void
    {
        if (_memberObj != null) {
            _memberObj.removeListener(this);
            _memberObj = null;
            regenerateAllShown();
        }
    }

    /**
     * Add an item that will be shown alongside the user's inventory.
     */
    public function addFakeItem (item :Item) :void
    {
        _fakeItems.push(item);
        if (isActive()) {
            regenerateAllShown();
        }
    }

    /**
     * Called by an InventoryPicker when a particular item type has been
     * opened.
     */
    public function typeOpened (treeNode :Object) :void
    {
        var type :int = int(treeNode.itemType);
        if (type != Item.NOT_A_TYPE) {
            loadType(type);
        }
    }

    /**
     * Request that the specified item type be loaded.
     */
    public function loadType (type :int) :void
    {
        _ctx.getItemDirector().loadInventory(type);
    }

    /**
     * Are we showing 'used' items?
     */
    public function isShowUsed () :Boolean
    {
        return _showUsed;
    }

    /**
     * Set whether we're showing all used items.
     */
    public function setShowUsed (showUsed :Boolean) :void
    {
        if (_showUsed == showUsed) {
            return; // no change
        }

        _showUsed = showUsed;
        regenerateAllShown();
    }

    /**
     * Set the item types that we're showing.
     */
    public function setShownTypes (types :Array) :void
    {
        if (Util.equals(types, _shownTypes)) {
            return; // no change
        }

        // first, flush each list (release item references)
        for each (var showList :ArrayCollection in _showItems) {
            showList.removeAll();
        }

        _shownTypes = types;
        // if we're only showing one type, load that type now
        if (_shownTypes.length == 1 && isActive()) {
            loadType(int(_shownTypes[0]));
        }
        regenerateAllShown();
        this.refresh(); // we've changed the filter
    }

    // from AttributeChangeListener
    public function attributeChanged (evt :AttributeChangedEvent) :void
    {
        if (evt.getName() != MemberObject.LOADED_INVENTORY) {
            return;
        }

        var types :int = (evt.getValue() as int);
        var oldTypes :int = (evt.getOldValue() as int);

        // determine which types changed, and update those
        for (var ii :int = 0; ii < 32; ii++) {
            var typeMask :int = (1 << ii);
            if ((types & typeMask) != (oldTypes & typeMask)) {
                regenerateShown(ii);
            }
        }
    }

    // from SetListener
    public function entryAdded (evt :EntryAddedEvent) :void
    {
        if (evt.getName() != MemberObject.INVENTORY) {
            return;
        }
        var type :int = (evt.getEntry() as Item).getType();
        // hold off on updating any newly added entries if the loadedInventory
        // flag for that type is not yet set.
        if (_memberObj.isInventoryLoaded(type)) {
            regenerateShown(type);
        }
    }

    // from SetListener
    public function entryRemoved (evt :EntryRemovedEvent) :void
    {
        if (evt.getName() != MemberObject.INVENTORY) {
            return;
        }
        var type :int = (evt.getOldEntry() as Item).getType();
        regenerateShown(type);
    }

    // from SetListener
    public function entryUpdated (evt :EntryUpdatedEvent) :void
    {
        if (evt.getName() != MemberObject.INVENTORY) {
            return;
        }
        var type :int = (evt.getOldEntry() as Item).getType();
        regenerateShown(type);
    }

    override public function toString () :String
    {
        return "InventoryCollectionView: " + super.toString();
    }

    /**
     * Regenerate all the shown lists for all showing item categories.
     */
    protected function regenerateAllShown () :void
    {
        for each (var type :int in _shownTypes) {
            regenerateShown(type);
        }
    }

    /**
     * Completely regenerate the shown list for a particular item type.
     * Dispatch no events...
     */
    protected function regenerateShown (type :int) :void
    {
        var showList :ArrayCollection = (_showItems[type] as ArrayCollection);
        if (_memberObj == null) {
            showList.removeAll(); // don't retain references
            return; // not now!
        }

        if (!_memberObj.isInventoryLoaded(type)) {
            if (showList.length == 0) {
                showList.addItem(Msgs.ITEM.get("m.retrieving"));
            }
            return; // not loaded, ignore
        }

        var rawList :Array = _memberObj.getItems(type);
        var item :Item;

        showList.disableAutoUpdate();
        try {
            showList.removeAll();

            // add all the inventory items
            for each (item in rawList) {
                if (_showUsed || !item.isUsed()) {
                    showList.addItem(item);
                }
            }

            // add any "fake" items of the right type
            for each (item in _fakeItems) {
                if (item.getType() == type) {
                    showList.addItem(item);
                }
            }

            // if nothing was generated, be sure to add a "nuffink" label
            if (showList.length == 0) {
                showList.addItem(Msgs.ITEM.get((rawList.length > 0)
                    ? "m.no_filtered_items" : "m.no_items"));
            }

        } finally {
            showList.enableAutoUpdate();
        }
    }

    /**
     * Filter function for this top-level collection.
     */
    protected function filterCategories (thing :Object) :Boolean
    {
        var type :int = int(thing.itemType);
        return (-1 != _shownTypes.indexOf(type));
    }

    /** The context, holy giver of life. */
    protected var _ctx :MsoyContext;

    /** The member object we're using as the source for our inventory. */
    protected var _memberObj :MemberObject;

    /** Whether or not to show 'used' items. */
    protected var _showUsed :Boolean;

    /** Contains the categories we're currently showing. */
    protected var _shownTypes :Array;

    /** Contains an ArrayCollection for each category, containing the
     * Currently shown items for that category. */
    protected var _showItems :Array = [];

    /** Any non-inventory items added alongside the "real" ones. */
    protected var _fakeItems :Array = [];
}
}
