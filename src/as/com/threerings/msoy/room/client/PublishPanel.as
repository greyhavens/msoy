//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.ErrorEvent;
import flash.events.Event;

import com.threerings.util.CommandEvent;
import com.threerings.util.Log;

import com.threerings.flex.FlexUtil;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.client.snapshot.Snapshot;

/**
 * Asks the player if they want to publish their room.
 */
public class PublishPanel extends FloatingPanel
{
    public function PublishPanel (ctx :WorldContext, view :RoomObjectView)
    {
        super(ctx, Msgs.WORLD.get("t.publish"));
        _view = view;
        styleName = "sexyWindow";
        showCloseButton = true;

        _snapshot = Snapshot.createThumbnail(ctx, view, handleSnapshotReady, handleUploadError);
        _snapshot.updateSnapshot(false, false, true);

        open();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(FlexUtil.createLabel(Msgs.WORLD.get("l.publish_room"))); 
        addButtons(OK_BUTTON, CANCEL_BUTTON);
        getButton(OK_BUTTON).enabled = _snapshot.ready;
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        if (buttonId == OK_BUTTON) {
            _snapshot.upload();
            CommandEvent.dispatch(_view, RoomObjectController.PUBLISH_ROOM);
        }

        super.buttonClicked(buttonId);
    }

    protected function handleSnapshotReady (event :Event) :void
    {
        getButton(OK_BUTTON).enabled = true;
    }

    protected function handleUploadError (event :ErrorEvent) :void
    {
        Log.getLog(this).warning("Snapshot upload error", "error", event.text);
    }

    protected var _view :RoomObjectView;

    protected var _snapshot :Snapshot;
}
}
