//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.data.all.MemberName;

/**
 * Used for two purposes:
 *  - Generate a notification purely in the client, from the NotificationDirector
 *    noticing some change on the MemberObject or something.
 *  - Sending a notification to the client without creating a custom class. Custom classes
 *    are often preferred since they can compress the notification data.
 *
 * If you need to specify a sender, it's time to write a custom class, brah.
 */
public class GenericNotification extends Notification
{
    public function GenericNotification (
        msg :String = null, category :int = 0, sender :MemberName = null)
    {
        _msg = msg;
        _cat = category;
        _sender = sender;
    }

    override public function getAnnouncement () :String
    {
        return _msg;
    }

    override public function getCategory () :int
    {
        return _cat;
    }

    override public function getSender () :MemberName
    {
        return _sender;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        _msg = ins.readField(String) as String;
        _cat = ins.readByte();
        // _sender cannot come from the server
    }

    protected var _msg :String;
    protected var _cat :int;
    protected var _sender :MemberName;
}
}
