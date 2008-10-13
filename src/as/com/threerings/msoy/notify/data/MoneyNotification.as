//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.util.MessageBundle;

import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.money.data.all.Currency;

/**
 * Base class for money notifications.
 */
public class MoneyNotification extends Notification
{
    /** The type of money this notification is concerned with. */
    public var currency :Currency;

    /** The amount that it's changed. */
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
            if (amt < 0) {
                key = "m.money_lost";
                amt = -amt; // positivity!
            } else {
                key = "m.money_gained";
            }
        }

        return MessageBundle.compose(key, currency.compose(amt));
    }

    override public function getCategory () :int
    {
        return PERSONAL;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        currency = Currency(ins.readObject());
        delta = ins.readInt();
        _msg = ins.readField(String) as String;
    }

    /** The translation key, or null to use a generic message. */
    protected var _msg :String;
}
}
