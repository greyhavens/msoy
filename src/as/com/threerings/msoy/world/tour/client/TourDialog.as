//
// $Id$

package com.threerings.msoy.world.tour.client {

import mx.containers.HBox;
import mx.containers.VBox;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.world.client.WorldContext;

public class TourDialog extends FloatingPanel
{
    public function TourDialog (ctx :WorldContext, nextRoom :Function)
    {
        super(ctx, Msgs.WORLD.get("t.tour"));
        _wctx = ctx;
        showCloseButton = true;

        var nextBtn :CommandButton = new CommandButton(Msgs.WORLD.get("b.tour_next"), nextRoom);

        var commentBtn :CommandButton = new CommandButton(null, MsoyController.VIEW_COMMENT_PAGE);
        commentBtn.styleName = "controlBarButtonComment"
        commentBtn.toolTip = Msgs.GENERAL.get("i.comment");

        var hbox :HBox = new HBox();
        hbox.addChild(nextBtn);
        hbox.addChild(commentBtn);

        addChild(hbox);
    }

    protected var _wctx :WorldContext;
}

}
