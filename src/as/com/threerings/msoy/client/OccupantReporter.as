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

//        if (reportInitialOccupants) {
//            // report the current room occupants
//            var occs :String = "";
//            for (var iter :Iterator = plobj.occupantInfo.iterator(); iter.hasNext(); ) {
//                var info :OccupantInfo = (iter.next() as OccupantInfo);
//                if (isSelf(info)) {
//                    continue;
//                }
//                if (occs.length > 0) {
//                    occs += ", ";
//                }
//                occs += info.username;
//            }
//            if (occs.length > 0) {
//                _ctx.getChatDirector().displayInfo(MsoyCodes.GENERAL_MSGS,
//                    MessageBundle.tcompose("m.in_room", occs));
//            }
//        }
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
            if (isSelf(info)) {
                return;
            }
            // TODO: if we keep this, move the translation to the notify bundle
            _ctx.getNotificationDirector().addGenericNotification(
                MessageBundle.tcompose("m.entered_room", info.username),
                Notification.LOWEST, info.username as MemberName);
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
            if (isSelf(info)) {
                return;
            }
            // TODO: if we keep this, move the translation to the notify bundle
            _ctx.getNotificationDirector().addGenericNotification(
                MessageBundle.tcompose("m.left_room", info.username),
                Notification.LOWEST, info.username as MemberName);
        }
    }

    protected function isSelf (info :OccupantInfo) :Boolean
    {
        return info.username.equals(
            (_ctx.getClient().getClientObject() as BodyObject).getVisibleName());
    }

    protected var _ctx :MsoyContext;
}
}
