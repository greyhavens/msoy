//
// $Id$

package com.threerings.msoy.world.tour.client {

import mx.containers.HBox;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.world.client.WorldContext;

public class TourControl extends HBox
{
    public function TourControl (ctx :WorldContext, nextRoom :Function, endTour :Function)
    {
        const nextBtn :CommandButton = new CommandButton("next room", nextRoom);

        const commentBtn :CommandButton = new CommandButton(null, MsoyController.VIEW_COMMENT_PAGE);
        commentBtn.styleName = "controlBarButtonComment"
        commentBtn.toolTip = Msgs.GENERAL.get("i.comment");

        const closeBtn :CommandButton = new CommandButton(null, endTour);
        closeBtn.styleName = "closeButton";

        addChild(nextBtn);
        addChild(commentBtn);
        addChild(closeBtn);
    }
}
}
