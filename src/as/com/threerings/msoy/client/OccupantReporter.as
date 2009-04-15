//
// $Id$

package com.threerings.msoy.client {

import com.threerings.util.Iterator;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.notify.data.Notification;

/**
 * Listens for and reports occupant entry and exit.
 */
public class OccupantReporter
    implements SetListener
{
    public function willEnterPlace (ctx :MsoyContext, plobj :PlaceObject) :void
    {
        _ctx = ctx;
        // listen for and report enter/exit
        plobj.addListener(this);
    }

    public function didLeavePlace (plobj :PlaceObject) :void
    {
        plobj.removeListener(this);
    }

    // from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == PlaceObject.OCCUPANT_INFO) {
            var info :OccupantInfo = (event.getEntry() as OccupantInfo);
            notify("m.entered_room", info.username as MemberName);
        }
    }

    // from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        // nada
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        if (event.getName() == PlaceObject.OCCUPANT_INFO) {
            var info :OccupantInfo = (event.getOldEntry() as OccupantInfo);
            notify("m.left_room", info.username as MemberName);
        }
    }

    /**
     * Send the notification, as long as it's not about us.
     */
    protected function notify (key :String, name :MemberName) :void
    {
        if (name != null && !name.equals(_ctx.getMyName())) {
            _ctx.getNotificationDirector().addGenericNotification(
                MessageBundle.tcompose(key, name, name.getMemberId()),
                Notification.LOWEST, name);
        }
    }

    protected var _ctx :MsoyContext;
}
}
