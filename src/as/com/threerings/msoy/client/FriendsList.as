package com.threerings.msoy.client {

import mx.controls.List;
import mx.controls.listClasses.ListItemRenderer;

import mx.core.ClassFactory;

import com.threerings.util.ArrayUtil;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.NamedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.msoy.data.FriendEntry;
import com.threerings.msoy.data.MsoyUserObject;

public class FriendsList extends List
    implements SetListener
{
    public function FriendsList (ctx :MsoyContext)
    {
        super();
        _ctx = ctx;
        _userObj = (_ctx.getClient().getClientObject() as MsoyUserObject);
        _userObj.addListener(this);
        // TODO: remove listener?

        dataTipFunction = getTipFor;
        iconFunction = getIconFor;
        labelFunction = getLabelFor;
        //itemRenderer = new ClassFactory(FriendsItemRenderer);

        updateFriends();
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
        if (MsoyUserObject.FRIENDS === event.getName()) {
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
    }

    // our function for retrieving a label for an entry in the list
    protected function getLabelFor (obj :Object) :String
    {
        return (obj as FriendEntry).name.toString();
    }

    // our function for retrieving an icon for an entry in the list
    protected function getIconFor (obj :Object) :Class
    {
        return (obj as FriendEntry).online ? OpenEye : CloseEye;
    }

    // our function for retrieving a tooltip for an entry in the list
    protected function getTipFor (obj :Object) :String
    {
        // TODO: translate correctly
        var fe :FriendEntry = (obj as FriendEntry);
        return (fe.name.toString() + ": " + (fe.online ? "online" : "offline"));
    }

    /** The giver of life. */
    protected var _ctx :MsoyContext;

    /** Our own user object. */
    protected var _userObj :MsoyUserObject;
}
}

import mx.core.SpriteAsset;

import mx.controls.listClasses.ListItemRenderer;

//class FriendsItemRenderer extends ListItemRenderer
//{
//}

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

    override public function get measuredWidth () :Number
    {
        return 20;
    }

    override public function get measuredHeight () :Number
    {
        return 10;
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
