//
// $Id$

package com.threerings.msoy.notify.data {

// TODO
// placeholder class, to demonstrate things
public class ReleaseNotesNotification extends Notification
{
    override public function getAnnouncement () :String
    {
        return "m.new_release_notes";
    }
}
}
