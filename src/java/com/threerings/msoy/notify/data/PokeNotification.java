//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.util.ActionScript;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.orth.notify.data.Notification;

import com.threerings.msoy.data.all.MemberName;

/**
 * Notifies a user that they've been poked.
 */
@com.threerings.util.ActionScript(omit=true)
public class PokeNotification extends Notification
{
    public PokeNotification ()
    {
    }

    @ActionScript(omit=true)
    public PokeNotification (MemberName poker)
    {
        _poker = poker;
    }

    @Override
    public String getAnnouncement ()
    {
        return MessageBundle.tcompose("m.profile_poked", _poker.toString(), _poker.getId());
    }

    @Override
    public byte getCategory ()
    {
        return PERSONAL;
    }

    @Override
    public Name getSender ()
    {
        return _poker;
    }

    protected MemberName _poker;
}
