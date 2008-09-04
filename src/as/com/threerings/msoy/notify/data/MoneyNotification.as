//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.util.MessageBundle;

import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.money.data.all.Currency;

public class MoneyNotification extends Notification
{
    public var currency :Currency;

    public var delta :int;

    public function MoneyNotification (announcement :String = null)
    {
        _msg = announcement;
    }

    override public function getAnnouncement () :String
    {
        var key :String = _msg;
        var amt :int = delta;
        if (key == null) {
            // generic announcement
            if (delta < 0) {
                key = "m.money_lost";
                amt = -amt; // positivity!
            } else {
                key = "m.money_gained";
            }
        }

        // TODO: ack, mein gott, something's wrong with pluralization with composition
        return MessageBundle.compose(key, MessageBundle.tcompose(currency.getKey(), amt));
    }

    override public function getCategory () :int
    {
        return PERSONAL;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        currency = ins.readObject() as Currency;
        delta = ins.readInt();
        _msg = ins.readField(String) as String;
    }

    protected var _msg :String;
}
}
