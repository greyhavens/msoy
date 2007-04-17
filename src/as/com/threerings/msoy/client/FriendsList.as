package com.threerings.msoy.client {

import flash.display.DisplayObjectContainer;

import com.threerings.util.ArrayUtil;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.NamedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.msoy.ui.MsoyList;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

public class FriendsList extends MsoyList
    implements SetListener
{
    public function FriendsList (ctx :WorldContext)
    {
        super(ctx);
        includeInLayout = false;
        showDataTips = true;
    }

    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        super.parentChanged(p);

        if (p != null) {
            _userObj = _ctx.getMemberObject();
            _userObj.addListener(this);
            updateFriends();

        } else {
            _userObj.removeListener(this);
            _userObj = null;
            dataProvider = null;
        }
    }

    // from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        maybeUpdateFriends(event);
    }

    // from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        maybeUpdateFriends(event);
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        maybeUpdateFriends(event);
    }

    /**
     * If the specified event applies to our list of friends, update.
     */
    protected function maybeUpdateFriends (event :NamedEvent) :void
    {
        // TODO: we'll want to update only the items that have changed
        // in the standard data
        if (MemberObject.FRIENDS === event.getName()) {
            updateFriends();
        }
    }

    /**
     * Update the friends list.
     */
    protected function updateFriends () :void
    {
        var friends :Array = _userObj.friends.toArray();
        ArrayUtil.sort(friends);
        dataProvider = friends;

        itemsNeedMeasurement = true;
    }

    override public function itemToLabel (obj :Object) :String
    {
        return (obj as FriendEntry).name.toString();
    }

    override public function itemToIcon (obj :Object) :Class
    {
        var fe :FriendEntry = (obj as FriendEntry);
        return fe.online ? OpenEye : CloseEye;
    }

    override public function itemToDataTip (obj :Object) :String
    {
        var fe :FriendEntry = (obj as FriendEntry);
        return Msgs.GENERAL.get(
            (fe.online ? "m.friend_online" : "m.friend_offline"), fe.name);
    }

    /** Our own user object. */
    protected var _userObj :MemberObject;
}
}

import mx.core.SpriteAsset;

class CloseEye extends SpriteAsset
{
    public function CloseEye ()
    {
        graphics.clear();

        graphics.beginFill(0xFFFFFF);
        graphics.moveTo(0, 10);
        graphics.curveTo(10, 20, 20, 10);
        graphics.curveTo(10, 0, 0, 10);
        graphics.endFill();

        graphics.lineStyle(2, 0x000000);
        graphics.moveTo(0, 10);
        graphics.curveTo(10, 20, 20, 10);
    }

    protected function drawAsPending () :void
    {
        graphics.lineStyle(1, 0xFF0000, .5);
        graphics.moveTo(0, 5);
        graphics.lineTo(20, 5);
    }

    override public function get measuredWidth () :Number
    {
        return 20;
    }

    override public function get measuredHeight () :Number
    {
        return 10;
    }
}

class CloseEyePending extends CloseEye
{
    public function CloseEyePending ()
    {
        super();
        drawAsPending();
    }
}

class OpenEye extends CloseEye
{
    public function OpenEye ()
    {
        super();

        graphics.curveTo(10, 0, 0, 10);

        graphics.beginFill(0x0066FF);
        graphics.drawCircle(10, 10, 6);

        graphics.beginFill(0x000000);
        graphics.drawCircle(10, 10, 2);
    }
}

class OpenEyePending extends OpenEye
{
    public function OpenEyePending ()
    {
        super();
        drawAsPending();
    }
}
