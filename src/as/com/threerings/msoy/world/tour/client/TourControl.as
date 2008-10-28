//
// $Id$

package com.threerings.msoy.world.tour.client {

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Text;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.world.client.WorldContext;

public class TourControl extends HBox
{
    public function TourControl (ctx :WorldContext, nextRoom :Function, endTour :Function)
    {
        setStyle("backgroundColor", 0x3399cc);
        setStyle("backgroundAlpha", .25);
        setStyle("paddingLeft", 5);
        setStyle("paddingRight", 5);
        this.height = ControlBar.HEIGHT;

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

        const vpan :VBox = new VBox();
        vpan.setStyle("verticalGap", 0);
        vpan.addChild(FlexUtil.createSpacer(1, 3));
        const hpan :HBox = new HBox();
        hpan.addChild(nextBtn);
        hpan.addChild(commentBtn);
        hpan.addChild(closeBtn);
        vpan.addChild(hpan);

        addChild(label);
        addChild(vpan);
    }
}
}
