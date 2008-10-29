//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.ErrorEvent;
import flash.events.Event;

import mx.controls.Button;
import mx.controls.Text;

import com.threerings.util.CommandEvent;
import com.threerings.util.Log;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.client.snapshot.Snapshot;

/**
 * Asks the player if they want to publish their room.
 */
public class PublishPanel extends FloatingPanel
{
    public static const PUBLISH_BUTTON :int = -10;

    public function PublishPanel (ctx :WorldContext, view :RoomObjectView)
    {
        super(ctx, Msgs.EDITING.get("t.publish"));
        _view = view;
        showCloseButton = true;
        setButtonWidth(0);

        _snapshot = Snapshot.createThumbnail(ctx, view, handleSnapshotReady, handleUploadError);
        _snapshot.updateSnapshot(false, false, true);

        open();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(FlexUtil.createText(Msgs.EDITING.get("m.publish"), 300)); 

        if (MsoySceneModel(_view.getScene().getSceneModel()).accessControl !=
                MsoySceneModel.ACCESS_EVERYONE) {
            var text :Text = FlexUtil.createText(Msgs.EDITING.get("m.publish_private"), 300);
            text.setStyle("color", 0xFF0000);
            addChild(text);
        }

        addButtons(PUBLISH_BUTTON, CANCEL_BUTTON);
        getButton(PUBLISH_BUTTON).enabled = _snapshot.ready;
    }

    override protected function getButtonLabel (buttonId :int) :String
    {
        switch (buttonId) {
        case CANCEL_BUTTON: return Msgs.EDITING.get("b.publish_cancel");
        case PUBLISH_BUTTON: return Msgs.EDITING.get("b.publish");
        default: return super.getButtonLabel(buttonId);
        }
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        if (buttonId == PUBLISH_BUTTON) {
            _snapshot.upload();
            CommandEvent.dispatch(_view, RoomObjectController.PUBLISH_ROOM);
        }

        close();
    }

    protected function handleSnapshotReady (event :Event) :void
    {
        getButton(PUBLISH_BUTTON).enabled = true;
    }

    protected function handleUploadError (event :ErrorEvent) :void
    {
        Log.getLog(this).warning("Snapshot upload error", "error", event.text);
    }

    protected var _view :RoomObjectView;

    protected var _snapshot :Snapshot;
}
}
