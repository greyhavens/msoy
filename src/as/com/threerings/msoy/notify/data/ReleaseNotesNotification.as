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

    override public function getDisplay () :UIComponent
    {
        var txt :Text = new Text();
        txt.text = "These are the new cool release notes, bla de bla.";
        txt.percentWidth = 100;
        return txt;
    }
}
}
