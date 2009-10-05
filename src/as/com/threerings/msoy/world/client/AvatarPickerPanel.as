//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.controls.Label;
import mx.core.ScrollPolicy;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.MsoyMediaContainer;

import com.threerings.util.Log;

/**
 * Panel to show a list of avatar buttons, close on click and return the selected one.
 * TODO: basic functionality: close on click, return the selection
 * TODO: show names of avatars
 * TODO: center the avatars in their slot
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
        addChild(tbox);

        for each (var avi :Avatar in _avatars) {
            var container :MsoyMediaContainer = new MsoyMediaContainer(avi.avatarMedia);
            tbox.addChild(new MediaWrapper(container, AVI_SIZE, AVI_SIZE, true));
        }

        var select :Label = new Label();
        select.styleName = "avatarPickerSelect";
        addChild(select);

        // listen on the HBox because listening on the thumbnails doesn't work
        tbox.addEventListener(MouseEvent.CLICK, function (evt :MouseEvent) :void {
            var idx :int = evt.localX / AVI_SIZE;
            if (idx >= 0 && idx < _avatars.length) {
                close();
                _select(_avatars[idx]);
            }
        });

        tbox.addEventListener(MouseEvent.MOUSE_MOVE, function (evt :MouseEvent) :void {
            var idx :int = evt.localX / AVI_SIZE;
            if (idx >= 0 && idx < _avatars.length) {
                select.text = "Select " + _avatars[idx].name;
            } else {
                select.text = "";
            }
        });
    }

    protected var _avatars :Array /* of Avatar */;
    protected var _select :Function;

    protected static const AVI_SIZE :int = 200; // NB: needs to fit in ~745 width for Facebook
}
}
