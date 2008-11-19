//
// $Id$

package com.threerings.msoy.world.tour.client {

import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.containers.VBox;

import com.threerings.util.CommandEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.data.all.RatingResult;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.Stars;
import com.threerings.msoy.ui.StarsEvent;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.room.client.RoomObjectView;

import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldController;
import com.threerings.msoy.room.client.RoomObjectView;

public class TourDialog extends FloatingPanel
{
    public function TourDialog (ctx :WorldContext, nextRoom :Function)
    {
        super(ctx, Msgs.WORLD.get("t.tour"));
        showCloseButton = true;

        var nextBtn :CommandButton = new CommandButton(Msgs.WORLD.get("b.tour_next"), nextRoom);

        var commentBtn :CommandButton = new CommandButton(null, MsoyController.VIEW_COMMENT_PAGE);
        commentBtn.styleName = "controlBarButtonComment"
        commentBtn.toolTip = Msgs.GENERAL.get("i.comment");

        var hbox :HBox = new HBox();
        hbox.addChild(nextBtn);
        hbox.addChild(commentBtn);

        if (DeploymentConfig.devDeployment) {
            _myStars = new Stars(0, Stars.USER_LEFT, Stars.USER_RIGHT);
            _myStars.addEventListener(Stars.STAR_CLICK, handleRate);
            _myStars.addEventListener(Stars.STAR_OVER, function (event :StarsEvent) :void {
                _myStars.setRating(event.rating);
            });
            _myStars.addEventListener(MouseEvent.ROLL_OUT, function (event :MouseEvent) :void {
                _myStars.setRating(_myRating);
            });
            hbox.addChild(_myStars);
        }

        addChild(hbox);
    }

    public function setRating (rating :Number) :void
    {
        _myRating = rating;
        _myStars.setRating(rating);
    }

    protected function handleRate (event :StarsEvent) :void
    {
        setRating(event.rating);
        CommandEvent.dispatch(this, WorldController.ROOM_RATE, event.rating);
    }

    protected var _myStars :Stars;
    protected var _myRating :Number;
}

}
