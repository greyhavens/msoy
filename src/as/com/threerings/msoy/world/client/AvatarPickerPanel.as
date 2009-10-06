//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.geom.Point;
import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.controls.Label;
import mx.core.ScrollPolicy;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.util.Log;

/**
 * Panel to show a list of avatar buttons, close on click and return the selected one.
 * TODO: a "next page" button (not scroll bars, we expect only 3-6 avatars to choose from)
 * TODO: pass in extra "gender" information, correlate to user's gender and allow changing
 * TODO: show more information in a tool tip, description, maybe number of states and actions
 */
public class AvatarPickerPanel extends FloatingPanel
{
    public static const log :Log = Log.getLog(AvatarPickerPanel);

    public static function showTest (ctx :WorldContext) :void
    {
        var avatars :Array = new Array();
        var test :Avatar = new Avatar();
        // this is a local hash code, just for testing and will be removed
        test.avatarMedia = new MediaDesc(MediaDesc.stringToHash(
            "c44c5cc6e3965fcc3475e37ccf8110283117cb54"), MediaDesc.APPLICATION_SHOCKWAVE_FLASH);
        avatars.push(test);
        avatars.push(test);
        avatars.push(test);
        show(ctx, avatars, function (avatar: Avatar) :void {
            log.info("Avatar selected", "name", avatar.name);
        });
    }

    /**
     * Show the array of avatars.
     */
    public static function show (ctx :WorldContext, avatars :Array /* of Avatar */,
                                 select :Function) :void
    {
        new AvatarPickerPanel(ctx, avatars, select).open();
    }

    /**
     * Creats a new picker.
     */
    public function AvatarPickerPanel (ctx :WorldContext, avatars :Array /* of Avatar */,
                                       select :Function) :void
    {
        super(ctx, "Select an avatar");
        _avatars = avatars;
        _select = select;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var instructions :Label = new Label();
        // TODO: title should get set depending on whether this is showing up for a theme boundary
        // or a home room creation
        instructions.text = "Welcome to your home on Whirled Rooms, select an avatar to wear.";
        instructions.styleName = "avatarPickerInstructions";
        addChild(instructions);

        var tbox :HBox = new HBox();
        tbox.styleName = "avatarPickerGrid";
        for each (var avi :Avatar in _avatars) {
            // ideally i'd be able to just use ROLL_OVER and ROLL_OUT events here on the media
            // wrapper, but that does not work
            tbox.addChild(MediaWrapper.createView(
                avi.getPreviewMedia(), MediaDesc.PREVIEW_SIZE));
        }
        addChild(tbox);

        var select :Label = new Label();
        select.styleName = "avatarPickerSelect";
        addChild(select);

        // Jesus fucking christ, surely a simple roll over and hit test can't be this much code.
        // I've spent over an hour trying to find out which fucking combination of events and
        // display objects and coordinate transforms to use and this disappointing monstrosity
        // is what I came up with

        function getSelected (evt :MouseEvent) :Avatar {
            var global :Point = new Point(evt.stageX, evt.stageY);
            var local :Point = tbox.globalToLocal(global);
            var children :Array = tbox.getChildren();
            for (var ii :int = 0; ii < children.length; ++ii) {
                var child :DisplayObject = children[ii];
                if (local.x >= child.x && local.x < child.x + child.width &&
                    local.y >= child.y && local.y < child.y + child.height) {
                    return _avatars[ii];
                }
            }
            return null;
        }

        // listen on the box because listening on the thumbnails doesn't work
        tbox.addEventListener(MouseEvent.CLICK, function (evt :MouseEvent) :void {
            if (getSelected(evt) != null) {
                close();
                _select(getSelected(evt));
            }
        });

        function updateSelection (evt :MouseEvent) :void {
            if (getSelected(evt) != null) {
                select.text = "Select " + getSelected(evt).name;
            } else {
                select.text = "";
            }
        }

        // listen for all 3 events to try and catch any change to the hover
        // NOTE the roll over event is not getting sent for some reason when the mouse goes out the
        // left edge of the box, so the select text stays set to the leftmost avatar name
        tbox.addEventListener(MouseEvent.ROLL_OVER, updateSelection);
        tbox.addEventListener(MouseEvent.ROLL_OUT, updateSelection);
        tbox.addEventListener(MouseEvent.MOUSE_MOVE, updateSelection);
    }

    protected var _avatars :Array /* of Avatar */;
    protected var _select :Function;
}
}
