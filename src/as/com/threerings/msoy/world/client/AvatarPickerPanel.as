//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.geom.Point;
import flash.events.Event;
import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Label;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.Msgs;
import com.threerings.orth.data.MediaDesc;
import com.threerings.msoy.data.all.MediaDescSize;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.util.Log;
import com.threerings.util.Util;

/**
 * Panel to show a list of avatar buttons, close on click and return the selected one.
 * TODO: a "next page" button (not scroll bars, we expect only 3-6 avatars to choose from)
 * TODO: pass in extra "gender" information, correlate to user's gender and allow changing
 * TODO: show more information in a tool tip, description, maybe number of states and actions
 */
public class AvatarPickerPanel extends FloatingPanel
{
    public static const log :Log = Log.getLog(AvatarPickerPanel);

    /**
     * Show the array of avatars.
     */
    public static function show (ctx :WorldContext, avatars :Array /* of Avatar */,
                                 tip :String, select :Function) :void
    {
        if (_picker != null) {
            return;
        }
        _picker = new AvatarPickerPanel(ctx, avatars, tip, select);
        _picker.open();
        _picker.addEventListener(DID_CLOSE, function (evt :Event) :void {
            _picker = null;
        });
    }

    /**
     * Show the array of avatars.
     */
    public static function close () :void
    {
        if (_picker == null) {
            return;
        }
        _picker.close();
    }

    /**
     * Creats a new picker.
     */
    public function AvatarPickerPanel (ctx :WorldContext, avatars :Array /* of Avatar */,
                                       tip :String, select :Function) :void
    {
        super(ctx, Msgs.WORLD.get("t.pick_avatar"));
        _avatars = avatars;
        _select = select;
        _tip = tip;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var instructions :Label = new Label();
        // TODO: title should get set depending on whether this is showing up for a theme boundary
        // or a home room creation
        instructions.text = _tip;
        instructions.styleName = "avatarPickerInstructions";
        addChild(instructions);

        var tbox :HBox = new HBox();
        tbox.styleName = "avatarPickerGrid";
        for each (var avi :Avatar in _avatars) {
            var icon :UIComponent;
            if (false) {
                icon = new UIComponent();
                var w :int = MediaDescSize.getWidth(MediaDescSize.PREVIEW_SIZE);
                var h :int = MediaDescSize.getHeight(MediaDescSize.PREVIEW_SIZE);
                icon.width = w;
                icon.height = h;
                icon.graphics.clear();
                icon.graphics.beginFill(0xff0000);
                icon.graphics.drawRect(0, 0, w, h);
                icon.graphics.endFill();

            } else {
                icon = MediaWrapper.createView(avi.getPreviewMedia(), MediaDescSize.PREVIEW_SIZE);
            }

            var select :CommandButton = new CommandButton(Msgs.WORLD.get("b.pick_avatar", avi.name),
                function (avatar :Avatar) :void {
                close();
                _select(avatar);
            }, avi);

            var cell :VBox = new VBox();
            cell.styleName = "avatarPickerCell";
            cell.addChild(icon);
            cell.addChild(select);
            tbox.addChild(cell);
        }
        addChild(tbox);
    }

    protected var _avatars :Array /* of Avatar */;
    protected var _select :Function;
    protected var _tip :String;

    protected static var _picker :AvatarPickerPanel;
}
}
