//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.Event;

import mx.containers.HBox;
import mx.controls.Label;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.world.client.WorldController;

public class TrackOverlay extends HBox
{
    public function TrackOverlay (ctx :MsoyContext, roomObj :RoomObject)
    {
        _ctx = ctx;
        _roomObj = roomObj;

        addEventListener(Event.ADDED_TO_STAGE, function (..._) :void {
            roomObj.trackRatingChanged.add(onRatingChanged);
            roomObj.trackChanged.add(onTrackChanged);
        });
        addEventListener(Event.REMOVED_FROM_STAGE, function (..._) :void {
            roomObj.trackRatingChanged.remove(onRatingChanged);
            roomObj.trackChanged.remove(onTrackChanged);
        });

        setStyle("bottom", 0);
        setStyle("right", 0);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _ratingLabel = new Label();
        _ratingLabel.width = 50;
        addChild(_ratingLabel);

        // TODO(bruno): Image button
        var rateUp :CommandButton = new CommandButton("+", rateTrack, true);
        rateUp.width = 32;
        rateUp.height = 32;
        addChild(rateUp);

        // TODO(bruno): Image button
        var rateDown :CommandButton = new CommandButton("-", rateTrack, false);
        rateDown.width = 32;
        rateDown.height = 32;
        addChild(rateDown);

        // TODO(bruno): Image button
        var showMusic :CommandButton = new CommandButton("M");
        showMusic.setCommand(WorldController.SHOW_MUSIC, showMusic);
        showMusic.width = 32;
        showMusic.height = 32;
        addChild(showMusic);
    }

    protected function onRatingChanged (rating :int) :void
    {
        _ratingLabel.text = (rating > 0 ? "+" : "") + rating;
    }

    protected function onTrackChanged () :void
    {
        _myRating = 0;
    }

    protected function rateTrack (like :Boolean) :void
    {
        var rating :int = like ? +1 : -1;
        if (rating == _myRating) {
            // Ignore a redundant click
            return;
        }

        _roomObj.roomService.rateTrack(_roomObj.track.audio.itemId, like);
        _myRating = rating;
    }

    protected var _ctx :MsoyContext;
    protected var _roomObj :RoomObject;

    protected var _ratingLabel :Label;
    protected var _myRating :int;
}
}
