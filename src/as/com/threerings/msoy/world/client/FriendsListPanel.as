//
// $Id$

package com.threerings.msoy.world.client {

import flash.geom.Rectangle;

import mx.containers.TitleWindow;

import mx.controls.List;

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

        }
    }

    // from SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        if (event.getName() == MemberObject.FRIENDS) {
            var newEntry :FriendEntry = event.getEntry() as FriendEntry;
            var oldEntry :FriendEntry = event.getOldEntry() as FriendEntry;
            if (newEntry.online != oldEntry.online) {
            } else if (newEntry.online) {
                // they changed something else, like their display name or profile image
            }
        }
    }

    // from SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        if (event.getName() == MemberObject.FRIENDS) {
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
        maxHeight = placeBounds.height - PADDING * 2;
        x = placeBounds.x + placeBounds.width - width - PADDING;
        y = placeBounds.y + PADDING;

        _friendList = new List();
        _friendList.styleName = "friendList";
        _friendList.horizontalScrollPolicy = ScrollPolicy.OFF;
        _friendList.verticalScrollPolicy = ScrollPolicy.AUTO;
        //_friendList.percentHeight = 100;
        _friendList.height = 200;
        _friendList.percentWidth = 100;
        addChild(_friendList);

        init(_ctx.getMemberObject());
    }

    protected function init (memObj :MemberObject) :void
    {
        memObj.addListener(this);
    }

    private static const log :Log = Log.getLog(FriendsListPanel);

    protected static const PADDING :int = 10;

    protected var _ctx :WorldContext;
    protected var _friendList :List;
}
}
