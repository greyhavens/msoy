//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.item.data.all.Item;

/**
 * Notifies a user that something they created has been commented on.
 */
public class EntityCommentedNotification extends Notification
{
    // from Notification
    override public function getAnnouncement () :String
    {
        if (_isRoom) {
            return MessageBundle.tcompose("m.room_commented", _entityName, _entityId);
        } else if (_isProfile) {
            return MessageBundle.tcompose("m.profile_commented", _entityId);
        } else {
            return MessageBundle.compose("m.item_commented",
                MessageBundle.qualify(MsoyCodes.ITEM_MSGS, Item.getTypeKey(_entityType)),
                MessageBundle.taint(_entityName), MessageBundle.taint(_entityType), 
                MessageBundle.taint(_entityId));
        }
    }

    // from Notification
    override public function getCategory () :int
    {
        return BUTTSCRATCHING;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _isRoom = ins.readBoolean();
        _isProfile = ins.readBoolean();
        _entityType = ins.readInt();
        _entityId = ins.readInt();
        _entityName = ins.readField(String) as String;
    }

    protected var _isRoom :Boolean;
    protected var _isProfile :Boolean;
    protected var _entityType :int;
    protected var _entityId :int;
    protected var _entityName :String;
}
}
