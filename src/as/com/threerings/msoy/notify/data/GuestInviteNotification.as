//
// $Id$

package com.threerings.msoy.notify.data {

import mx.controls.Text;

import mx.core.UIComponent;

import com.threerings.io.ObjectInputStream;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.client.Msgs;

/**
 * Notifies a guest that they've been given an invitation.
 */
public class GuestInviteNotification extends Notification
{
    // from Notification
    override public function getAnnouncement () :String
    {
        return MessageBundle.tcompose("m.guest_invite");
    }

    // from Notification
    override public function getDisplay () :UIComponent
    {
        var invite :Text = new Text();
        Log.getLog(this).debug(Msgs.GENERAL.get("m.invite_text", _serverUrl + "/#invite-" + _inviteId));
        invite.htmlText = Msgs.GENERAL.get("m.invite_text", _serverUrl + "/#invite-" + _inviteId);
        return invite;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _inviteId = ins.readField(String) as String;
        _serverUrl = ins.readField(String) as String;
    }

    protected var _inviteId :String;
    protected var _serverUrl :String;
}
}
