//
// $Id$

package com.threerings.msoy.world.tour.client {

import mx.containers.HBox;

import mx.controls.Text;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.world.client.WorldContext;

public class TourControl extends HBox
{
    public function TourControl (ctx :WorldContext, nextRoom :Function, endTour :Function)
    {
        setStyle("backgroundColor", 0x777777);

        const label :Text = new Text();
        label.selectable = false;
        label.setStyle("color", "white");
        label.setStyle("fontSize", 9);
        label.text = Msgs.WORLD.get("l.tour");

        const nextBtn :CommandButton = new CommandButton(Msgs.WORLD.get("b.tour_next"), nextRoom);

        const commentBtn :CommandButton = new CommandButton(null, MsoyController.VIEW_COMMENT_PAGE);
        commentBtn.styleName = "controlBarButtonComment"
        commentBtn.toolTip = Msgs.GENERAL.get("i.comment");

        const closeBtn :CommandButton = new CommandButton(null, endTour);
        closeBtn.styleName = "closeButton";

        addChild(label);
        addChild(nextBtn);
        addChild(commentBtn);
        addChild(closeBtn);
    }
}
}
