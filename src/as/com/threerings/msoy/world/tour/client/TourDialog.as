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

import com.threerings.msoy.world.tour.data.TourStop;

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
            _averageRating = new Stars(3.7, Stars.AVERAGE_LEFT, Stars.AVERAGE_RIGHT);
            hbox.addChild(_averageRating);

            _myRating = new Stars(3.7, Stars.USER_LEFT, Stars.USER_RIGHT);
            _myRating.addEventListener(Stars.STAR_CLICK, handleRate);
            _myRating.addEventListener(Stars.STAR_OVER, function (event :StarsEvent) :void {
                _myRating.setRating(event.rating);
            });
            _myRating.addEventListener(MouseEvent.MOUSE_OUT, function (event :MouseEvent) :void {
                _myRating.setRating(3.7);
            });
            hbox.addChild(_myRating);
        }

        addChild(hbox);
    }

    public function setStop (stop :TourStop) :void
    {
        // TODO: Rating count
        _averageRating.setRating(stop.rating.averageRating);
        _myRating.setRating(stop.rating.myRating);
    }

    protected function handleRate (event :StarsEvent) :void
    {
        _myRating.setRating(event.rating);
        (_ctx.getPlaceView() as RoomObjectView).getRoomController().rateRoom(event.rating,
            function (result :RatingResult) :void {
                // TODO: Rating count
                _averageRating.setRating(result.rating);
            });
    }

    protected var _averageRating :Stars;
    protected var _myRating :Stars;
}

}
