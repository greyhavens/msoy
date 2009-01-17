//
// $Id$

package com.threerings.msoy.room.client.editor {

import flash.events.ErrorEvent;
import flash.events.Event;

import mx.controls.Button;
import mx.controls.CheckBox;
import mx.controls.Text;

import com.threerings.util.CommandEvent;
import com.threerings.util.Log;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.client.RoomObjectController;
import com.threerings.msoy.room.client.RoomObjectView;
import com.threerings.msoy.room.client.RoomPostcardPanel;
import com.threerings.msoy.room.client.snapshot.Snapshot;
import com.threerings.msoy.room.data.MsoySceneModel;

/**
 * Asks the player if they want to publish their room.
 */
public class PublishPanel extends FloatingPanel
{
    public function PublishPanel (ctx :WorldContext, view :RoomObjectView)
    {
        super(ctx, Msgs.EDITING.get("t.publish"));
        _view = view;
        showCloseButton = true;
        setButtonWidth(0);

        _snapshot = Snapshot.createThumbnail(ctx, view, onSnapshotReady, onUploadError);
        _snapshot.updateSnapshot(false, false, true);

        open();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var msg :String = MsoySceneModel(_view.getScene().getSceneModel()).accessControl !=
            MsoySceneModel.ACCESS_EVERYONE ? "m.publish_private" : "m.publish";
        addChild(FlexUtil.createText(Msgs.EDITING.get(msg), 300));

        addChild(_postcard = new CheckBox());
        _postcard.label = Msgs.EDITING.get("l.publish_postcard");

        addButtons(OK_BUTTON, CANCEL_BUTTON);
        getButton(OK_BUTTON).enabled = _snapshot.ready;
    }

    override protected function getButtonLabel (buttonId :int) :String
    {
        switch (buttonId) {
        case OK_BUTTON: return Msgs.EDITING.get("b.publish");
        default: return super.getButtonLabel(buttonId);
        }
    }

    override protected function okButtonClicked () :void
    {
        _snapshot.upload(false, onUploadComplete);
        CommandEvent.dispatch(_view, RoomObjectController.PUBLISH_ROOM);
    }

    protected function onSnapshotReady (event :Event) :void
    {
        getButton(OK_BUTTON).enabled = true;
    }

    protected function onUploadError (event :ErrorEvent) :void
    {
        Log.getLog(this).warning("Snapshot upload error", "error", event.text);
    }

    protected function onUploadComplete (data :String) :void
    {
        if (_postcard.selected) {
            new RoomPostcardPanel(_ctx as WorldContext).open();
        }
    }

    protected var _view :RoomObjectView;
    protected var _postcard :CheckBox;
    protected var _snapshot :Snapshot;
}
}
