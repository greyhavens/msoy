//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.ErrorEvent;
import flash.events.Event;

import mx.containers.HBox;
import mx.controls.CheckBox;
import mx.controls.Image;
import mx.controls.Label;
import mx.controls.TextInput;
import mx.core.BitmapAsset;
import mx.core.UIComponent;

import com.threerings.io.TypedArray;
import com.threerings.util.MailUtil;
import com.threerings.util.StringUtil;

import com.threerings.presents.client.ConfirmAdapter;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.room.client.snapshot.Snapshot;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.world.client.WorldContext;

/**
 * Displays an interface for sending a room snapshot (canonical or otherwise) to your friends as a
 * postcard.
 */
public class RoomPostcardPanel extends FloatingPanel
{
    public function RoomPostcardPanel (ctx :WorldContext, snapURL :String = null)
    {
        super(ctx, Msgs.WORLD.get("t.rpc"));

        // save our snapshot URL if we have one
        _snapURL = snapURL;

        // specify a biggish min width/height so that the centered screen position we compute
        // before we know our image dimensions isn't too wildly different from what would be
        // correct; I don't know how to get Image to fire an event once it knows its real
        // dimensions, maybe someone with more Flex experience can fix
        minWidth = 500;
        minHeight = 300;

        // if we have no snap URL we're using the canonical scene image, so we fake a snapshot here
        // to give the user an idea of what will be showing
        if (_snapURL == null) {
            _snapshot = Snapshot.createThumbnail(
                ctx, ctx.getPlaceView() as RoomView, function (e :Event) :void {},
                function (e :ErrorEvent) :void {});
            _snapshot.updateSnapshot(false, false, false);
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(makeRow("l.rpc_to", _to = new TextInput()));
        var totip :Label = new Label();
        totip.text = Msgs.WORLD.get("t.rpc_to");
        addChild(makeRow("", totip));

        addChild(makeRow("l.rpc_subject", _subject = new TextInput()));
        _subject.text = Msgs.WORLD.get("m.rpc_def_subject");
        _subject.maxChars = MAX_SUBJECT_LENGTH;
        _subject.percentWidth = 100;

        addChild(makeRow("l.rpc_cc", _cc = new CheckBox()));
        _cc.label = Msgs.WORLD.get("l.rpc_cc_check");

        var preview :Image = new Image();
        preview.maxWidth = 600;
        preview.maxHeight = 200;
        if (_snapshot != null) {
            preview.source = new BitmapAsset(_snapshot.bitmap);
        } else {
            preview.source = _snapURL;
// TODO: is there some event that will be triggered when our image dimensions are known?
//             preview.addEventListener(FlexEvent.DATA_CHANGE, function (event :FlexEvent) :void {
//                 PopUpManager.centerPopUp(this);
//             });
        }
        addChild(makeRow("", preview));

        addChild(makeRow("l.rpc_caption", _caption = new TextInput()));
        _caption.text = Msgs.WORLD.get("m.rpc_def_caption");
        _caption.percentWidth = 100;

        addChild(_status = new Label());
        setStatus("");

        addButtons(OK_BUTTON, CANCEL_BUTTON);
    }

    override protected function getButtonLabel (buttonId :int) :String
    {
        switch (buttonId) {
        case OK_BUTTON: return Msgs.WORLD.get("b.rpc_send");
        default: return super.getButtonLabel(buttonId);
        }
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        if (buttonId == OK_BUTTON) {
            sendPostcard();
        } else {
            super.buttonClicked(buttonId);
        }
    }

    protected function makeRow (label :String, control :UIComponent) :HBox
    {
        var row :HBox = new HBox();
        row.percentWidth = 100;
        row.setStyle("verticalAlign", "middle");
        var lbl :Label = new Label();
        if (label.length > 0) {
            lbl.text = Msgs.WORLD.get(label);
        }
        lbl.selectable = false;
        lbl.setStyle("textAlign", "right");
        lbl.width = 60;
        row.addChild(lbl);
        row.addChild(control);
        control.percentWidth = 100;
        return row;
    }

    protected function sendPostcard () :void
    {
        var addrs :TypedArray = TypedArray.create(String);
        addrs.addAll(StringUtil.trim(_to.text).split(/[, ]+/));
        var subject :String = StringUtil.trim(_subject.text);
        var caption :String = StringUtil.trim(_caption.text);

        if (addrs.length == 0 || addrs[0].length == 0) {
            setStatus(Msgs.WORLD.get("e.rpc_enter_email"));
            return;
        }
        for each (var addr :String in addrs) {
            if (!MailUtil.isValidAddress(addr)) {
                setStatus(Msgs.WORLD.get("e.rpc_invalid_addr", addr));
                return;
            }
        }

        if (subject.length == 0) {
            setStatus(Msgs.WORLD.get("e.rpc_enter_subject"));
            return;
        }

        // add our own email address if we have _cc checked
        if (_cc.selected) {
            addrs.addAll([ (_ctx as WorldContext).getMemberObject().username.toString() ]);
        }

        setStatus(Msgs.WORLD.get("m.rpc_sending"));
        _buttons[OK_BUTTON].enabled = false;

        (_ctx.getLocationDirector().getPlaceObject() as RoomObject).roomService.sendPostcard(
            _ctx.getClient(), addrs, subject, caption, _snapURL,
            new ConfirmAdapter(function () :void {
                _ctx.displayFeedback(MsoyCodes.WORLD_MSGS, "m.rpc_sent");
                close();
            }, function (reason :String) :void {
                _buttons[OK_BUTTON].enabled = true;
                setStatus(_ctx.xlate(MsoyCodes.WORLD_MSGS, reason));
            }));
    }

    protected function setStatus (status :String) :void
    {
        _status.text = status;
        _status.visible = _status.includeInLayout = (status != "");
    }

    protected var _to :TextInput;
    protected var _subject :TextInput;
    protected var _cc :CheckBox;
    protected var _caption :TextInput;
    protected var _status :Label;

    protected var _snapURL :String;
    protected var _snapshot :Snapshot;

    protected static const MAX_SUBJECT_LENGTH :int = 100;
}
}
