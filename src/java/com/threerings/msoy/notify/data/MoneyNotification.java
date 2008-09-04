//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.money.data.all.Currency;

/**
 * Base class for money notifications.
 */
public class MoneyNotification extends Notification
{
    /** The type of money this notification is concerned with. */
    public Currency currency;

    /** The amount that it's changed. */
    public int delta;

    public MoneyNotification ()
    {
    }

    public MoneyNotification (String announcement)
    {
        _msg = announcement;
    }

    @Override
    public String getAnnouncement ()
    {
        String key = _msg;
        int amt = delta;

        if (key == null) {
            // generic announcement
            if (amt < 0) {
                key = "m.money_lost";
                amt = -amt; // positivity!
            } else {
                key = "m.money_gained";
            }
        }

        return MessageBundle.compose(key, MessageBundle.tcompose(currency.getKey(), amt));
    }

    /** The translation key, or null to use a generic message. */
    protected String _msg;
}
