//
// $Id$

package com.threerings.msoy.room.client {

import com.threerings.flex.FlexUtil;
import com.threerings.util.CommandEvent;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldController;

/**
 * Asks the player if they want to publish their room.
 */
public class PublishPanel extends FloatingPanel
{
    public function PublishPanel (ctx :WorldContext)
    {
        super(ctx, Msgs.WORLD.get("t.publish"));
        styleName = "sexyWindow";
        showCloseButton = true;
        open();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(FlexUtil.createLabel(Msgs.WORLD.get("l.publish_room"))); 
        addButtons(OK_BUTTON, CANCEL_BUTTON);
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        super.buttonClicked(buttonId);

        if (buttonId == OK_BUTTON) {
            CommandEvent.dispatch(this, WorldController.PUBLISH_ROOM);
        }
    }
}
}
