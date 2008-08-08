//
// $Id$

package com.threerings.msoy.notify.data {

import mx.core.UIComponent;

import mx.controls.Text;

// TODO
// placeholder class, to demonstrate things
public class ReleaseNotesNotification extends Notification
{
    override public function getAnnouncement () :String
    {
        return "m.new_release_notes";
    }

    // from Notification
    override public function getCategory () :int
    {
        return BUTTSCRATCHING;
    }
}
}
