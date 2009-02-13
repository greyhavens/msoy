//
// $Id$

package com.threerings.msoy.world.tour.client {

import flash.events.MouseEvent;
import flash.net.URLRequest;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Image;
import mx.controls.Text;

import com.threerings.util.CommandEvent;
import com.threerings.util.ValueEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.data.all.RatingResult;

import com.threerings.msoy.ui.FlyingPanel;
import com.threerings.msoy.ui.Stars;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.room.client.RoomObjectView;

import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldController;
import com.threerings.msoy.room.client.RoomObjectView;

public class TourDialog extends FlyingPanel
{
    public function TourDialog (ctx :WorldContext)
    {
        super(ctx, Msgs.WORLD.get("t.tour"));
        showCloseButton = true;

        var nextBtn :CommandButton = new CommandButton(null);
        nextBtn.setCallback(ctx.getTourDirector().nextRoom, nextBtn);
        nextBtn.styleName = "tourNext";

        var commentBtn :CommandButton = new CommandButton(null, MsoyController.VIEW_COMMENT_PAGE);
        commentBtn.styleName = "controlBarButtonComment"
        commentBtn.toolTip = Msgs.GENERAL.get("i.comment");

        _myStars = new Stars(0, Stars.USER_LEFT, Stars.USER_RIGHT);
        _myStars.addEventListener(Stars.STAR_CLICK, handleRate);
        _myStars.addEventListener(Stars.STAR_OVER, function (event :ValueEvent) :void {
            _myStars.setRating(Number(event.value));
        });
        _myStars.addEventListener(MouseEvent.ROLL_OUT, function (event :MouseEvent) :void {
            _myStars.setRating(_myRating);
        });

        var hbox :HBox = new HBox();
        hbox.addChild(FlexUtil.createLabel(Msgs.WORLD.get("l.tour_rate")));
        hbox.addChild(_myStars);
        hbox.addChild(commentBtn);

        addChild(nextBtn);
        addChild(hbox);
    }

    public function setRating (rating :Number) :void
    {
        _myRating = rating;
        _myStars.setRating(rating);
    }

    protected function handleRate (event :ValueEvent) :void
    {
        setRating(int(event.value));
        CommandEvent.dispatch(this, WorldController.ROOM_RATE, event.value);
    }

    protected var _myStars :Stars;
    protected var _myRating :Number;
}
}
