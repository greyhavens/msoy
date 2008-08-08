//
// $Id$

package com.threerings.msoy.notify.data {

/**
 * Used to pass notification data around in the client.
 */
public class GenericNotification extends Notification
{
    public function GenericNotification (msg :String, category :int)
    {
        _msg = msg;
        _cat = category;
    }

    override public function getAnnouncement () :String
    {
        return _msg;
    }

    override public function getCategory () :int
    {
        return _cat;
    }

    protected var _msg :String;
    protected var _cat :int;
}
}
