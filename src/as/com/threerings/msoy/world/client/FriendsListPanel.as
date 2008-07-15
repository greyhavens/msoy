//
// $Id$

package com.threerings.msoy.world.client {

import flash.geom.Rectangle;

import flash.utils.Dictionary;

import mx.collections.ArrayCollection;
import mx.collections.Sort;

import mx.containers.TitleWindow;

import mx.controls.List;

import mx.core.ClassFactory;
import mx.core.ScrollPolicy;

import mx.managers.PopUpManager;

import mx.events.CloseEvent;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.util.Log;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

public class FriendsListPanel extends TitleWindow
    implements SetListener
{
    public function FriendsListPanel (ctx :WorldContext) :void
    {
        _ctx = ctx;

        addEventListener(CloseEvent.CLOSE, _ctx.getWorldController().handlePopFriendsList);
    }

    public function show () :void
    {
        PopUpManager.addPopUp(this, _ctx.getTopPanel(), false);
    }

    public function shutdown () :void
    {
        _ctx.getMemberObject().removeListener(this);
        PopUpManager.removePopUp(this);
    }

    public function memberObjectUpdated (memObj :MemberObject) :void
    {
        init(memObj);
    }

    // from SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == MemberObject.FRIENDS) {
            addFriend(event.getEntry() as FriendEntry);
        }
    }

    // from SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        if (event.getName() == MemberObject.FRIENDS) {
            var newEntry :FriendEntry = event.getEntry() as FriendEntry;
            var oldEntry :FriendEntry = event.getOldEntry() as FriendEntry;
            removeFriend(oldEntry);
            if (newEntry.online) {
                addFriend(newEntry);
            }
        }
    }

    // from SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        if (event.getName() == MemberObject.FRIENDS) {
            removeFriend(event.getOldEntry() as FriendEntry);
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // styles and positioning
        styleName = "friendsListPanel";
        showCloseButton = true;
        width = 219; // width of the friends online header image
        var placeBounds :Rectangle = _ctx.getTopPanel().getPlaceViewBounds(); 
        // TODO: react to client size changes, both width and height
        height = placeBounds.height - PADDING * 2;
        x = placeBounds.x + placeBounds.width - width - PADDING;
        y = placeBounds.y + PADDING;

        _friendsList = new List();
        _friendsList.styleName = "friendList";
        _friendsList.horizontalScrollPolicy = ScrollPolicy.OFF;
        _friendsList.percentWidth = 100;
        _friendsList.percentHeight = 100;
        _friendsList.itemRenderer = new ClassFactory(FriendRenderer);
        _friendsList.dataProvider = _friends;
        _friendsList.selectable = false;
        addChild(_friendsList);

        // set up the sort for the collection
        var sort :Sort = new Sort();
        sort.compareFunction = sortFunction;
        _friends.sort = sort;
        _friends.refresh();

        init(_ctx.getMemberObject());
    }

    protected function init (memObj :MemberObject) :void
    {
        memObj.addListener(this);

        var currentEntries :Array = _friends.toArray();
        for each (var friend :FriendEntry in memObj.friends.toArray()) {
//            if (!friend.online) {
//                continue;
//            }

            if (!containsFriend(friend)) {
                addFriend(friend);
            } else {
                var original :Object = _originals[friend.name.getMemberId()];
                currentEntries.splice(currentEntries.indexOf(original), 1);
            }
        }

        // anything left in currentEntries wasn't found on the new MemberObject
        for each (var entry :Array in currentEntries) {
            removeFriend(entry[1] as FriendEntry);
        }
    }

    protected function sortFunction (o1 :Object, o2 :Object, fields :Array = null) :int
    {
        if (!(o1 is FriendEntry) || !(o2 is FriendEntry)) {
            return 0;
        }

        var friend1 :FriendEntry = o1 as FriendEntry;
        var friend2 :FriendEntry = o2 as FriendEntry;
        return MemberName.BY_DISPLAY_NAME(friend1.name, friend2.name);
    }

    protected function containsFriend (friend :FriendEntry) :Boolean
    {
        return _originals[friend.name.getMemberId()] !== undefined;
    }

    protected function addFriend (friend :FriendEntry) :void
    {
        var data :Array = [ _ctx, friend ];
        _originals[friend.name.getMemberId()] = data;
        _friends.addItem(data);
    }

    protected function removeFriend (friend :FriendEntry) :void
    {
        var data :Array = _originals[friend.name.getMemberId()] as Array;
        if (data != null) {
            _friends.removeItemAt(_friends.getItemIndex(data));
        }
        delete _originals[friend.name.getMemberId()];
    }

    private static const log :Log = Log.getLog(FriendsListPanel);

    protected static const PADDING :int = 10;

    protected var _ctx :WorldContext;
    protected var _friendsList :List;
    protected var _friends :ArrayCollection = new ArrayCollection();
    protected var _originals :Dictionary = new Dictionary();
}
}
