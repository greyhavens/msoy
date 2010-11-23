//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.util.ActionScript;
import com.threerings.util.MessageBundle;

import com.threerings.orth.notify.data.Notification;

import com.threerings.msoy.comment.data.all.CommentType;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.item.data.all.Item;

/**
 * Notifies a user that something they created has been commented on.
 */
public class EntityCommentedNotification extends Notification
{
    public EntityCommentedNotification ()
    {
    }

    @ActionScript(omit=true)
    public EntityCommentedNotification (CommentType etype, int eid, String ename)
    {
        _entityType = etype;
        _entityId = eid;
        _entityName = ename;

        // need to determine these on the Java side because we don't have the Comment class
        // implemented in actionscript, and there is currently no other reason than these
        // constants to do so.
        _isRoom = etype.forRoom();
        _isProfile = etype.forProfileWall();
        _isGame = etype.forGame();
    }

    // from Notification
    public String getAnnouncement ()
    {
        if (_isRoom) {
            return MessageBundle.tcompose("m.room_commented", _entityName, _entityId);
        } else if (_isProfile) {
            return MessageBundle.tcompose("m.profile_commented", _entityId);
        } else if (_isGame) {
            return MessageBundle.tcompose("m.game_commented", _entityId);
        } else {
            return MessageBundle.compose("m.item_commented",
                MessageBundle.qualify(MsoyCodes.ITEM_MSGS, Item.getTypeKey(_entityType.toItemType())),
                _entityName, _entityType, _entityId);
        }
    }

    protected boolean _isRoom;
    protected boolean _isProfile;
    protected boolean _isGame;
    protected CommentType _entityType;
    protected int _entityId;
    protected String _entityName;
}
