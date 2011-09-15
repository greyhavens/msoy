//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.Event;

import caurina.transitions.Tweener;

import mx.containers.HBox;
import mx.controls.Label;

import com.threerings.util.F;
import com.threerings.util.RandomUtil;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.Track;
import com.threerings.msoy.world.client.WorldController;

public class TrackOverlay extends HBox
{
    public function TrackOverlay (ctx :MsoyContext, roomObj :RoomObject)
    {
        _ctx = ctx;
        _roomObj = roomObj;

        addEventListener(Event.ADDED_TO_STAGE, function (..._) :void {
            _roomObj.trackRatingChanged.add(onRatingChanged);
            _roomObj.trackChanged.add(onTrackChanged);
            _roomObj.messageReceived.add(onMessageReceived);
        });
        addEventListener(Event.REMOVED_FROM_STAGE, function (..._) :void {
            _roomObj.trackRatingChanged.remove(onRatingChanged);
            _roomObj.trackChanged.remove(onTrackChanged);
            _roomObj.messageReceived.remove(onMessageReceived);
        });

        setStyle("bottom", 0);
        setStyle("right", 0);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _ratingLabel = new Label();
        _ratingLabel.width = 50;
        _ratingLabel.setStyle("fontSize", 24);
        _ratingLabel.setStyle("textAlign", "right");
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

        onRatingChanged(_roomObj.trackRating);
        onTrackChanged(_roomObj.track);
    }

    protected function onMessageReceived (name :String, args :Array) :void
    {
        switch (name) {
        case RoomObject.TRACK_SKIPPED_MESSAGE:
            // TODO(bruno): Get a nicer looking effect for this
            var label :Label = new Label();
            label.text = Msgs.GENERAL.get("m.track_skipped");
            label.setStyle("fontSize", 24);
            label.includeInLayout = false;
            addChildAt(label, 0);

            Tweener.addTween(label, {
                time: 1, y: 80, onComplete: F.callback(removeChild, label)
            });
            break;
        }
    }

    protected function onRatingChanged (rating :int) :void
    {
        _ratingLabel.text = (rating > 0 ? "+" : "") + rating;
    }

    protected function onTrackChanged (newTrack :Track, oldTrack :Track = null) :void
    {
        _myRating = 0;

        if (newTrack == null) {
            // Ok, party's over
            playDefaultStateFrom(/\b(dj|dance)\b/i);

        } else if (amDj(newTrack)) {
            // Started DJ-ing, enter a DJ state (or dance if they don't have one)
            playState([ /\bdj\b/i, /\bdance\b/i ]);

        } else if (amDj(oldTrack)) {
            // Stopped DJ-ing, go back to just dancing
            playState([ /\bdance\b/i ]);
        }
    }

    protected function rateTrack (like :Boolean) :void
    {
        if (amDj(_roomObj.track)) {
            return; // You can't rate your own track
        }

        var rating :int = like ? +1 : -1;
        if (rating == _myRating) {
            return; // Ignore redundant clicks
        }

        if (like) {
            // Enter a new dance state
            playState([ /\bdance\b/i ]);
        } else {
            // Stop dancing
            playDefaultStateFrom(/\bdance\b/i);
        }

        _roomObj.roomService.rateTrack(_roomObj.track.audio.itemId, like);
        _myRating = rating;
    }

    protected function amDj (track :Track) :Boolean
    {
        return track != null && track.audio.ownerId == _ctx.getMyId();
    }

    protected function playState (patterns :Array) :void
    {
        var avatar :MemberSprite = RoomView(_ctx.getPlaceView()).getMyAvatar();
        var registeredStates :Array = avatar.getAvatarStates();
        var currentState :String = avatar.getState();

        for each (var pattern :RegExp in patterns) {
            var candidates :Array = [];
            var hasThisState :Boolean = false;

            for each (var state :String in registeredStates) {
                if (state.match(pattern)) {
                    hasThisState = true;
                    if (state != currentState) {
                        candidates.push(state);
                    }
                }
            }
            if (candidates.length > 0) {
                avatar.setState(RandomUtil.pickRandom(candidates));
            }

            if (hasThisState) {
                break; // Either we entered a matching state, or we were already in one
            }
        }
    }

    protected function playDefaultStateFrom (pattern :RegExp) :void
    {
        var avatar :MemberSprite = RoomView(_ctx.getPlaceView()).getMyAvatar();
        var currentState :String = avatar.getState();

        if (currentState.match(pattern)) {
            var registeredStates :Array = avatar.getAvatarStates();
            if (registeredStates.length > 0) {
                avatar.setState(registeredStates[0]);
            }
        }
    }

    protected var _ctx :MsoyContext;
    protected var _roomObj :RoomObject;

    protected var _ratingLabel :Label;
    protected var _myRating :int;
}
}
