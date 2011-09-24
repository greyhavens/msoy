//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.Event;

import caurina.transitions.Tweener;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Label;
import mx.controls.RadioButton;
import mx.controls.RadioButtonGroup;
import mx.core.ScrollPolicy;

import com.threerings.util.F;
import com.threerings.util.RandomUtil;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

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

        this.styleName = "trackOverlay";
        this.includeInLayout = false;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _skippedLabel = new Label();
        _skippedLabel.text = Msgs.WORLD.get("m.track_skipped");
        _skippedLabel.setStyle("fontSize", 24);
        _skippedLabel.visible = false;
        _skippedLabel.includeInLayout = false;
        addChild(_skippedLabel);

        _ratingControls = new HBox();
        _ratingControls.percentWidth = 100;

        _ratingLabel = new Label();
        _ratingLabel.width = 50;
        _ratingLabel.setStyle("textAlign", "right");
        var vbox :VBox = new VBox();
        vbox.setStyle("verticalAlign", "middle");
        vbox.addChild(_ratingLabel);
        vbox.height = 40;
        vbox.horizontalScrollPolicy = ScrollPolicy.OFF;
        vbox.verticalScrollPolicy = ScrollPolicy.OFF;
        _ratingControls.addChild(vbox);

        _radioGroup = new RadioButtonGroup();
        _radioGroup.addEventListener(Event.CHANGE, function (..._) :void {
            rateTrack(_radioGroup.selectedValue);
        });

        // var rateUp :CommandButton = newButton("rateUpButton", "i.rate_up");
        var rateUp :RadioButton = new RadioButton();
        rateUp.toolTip = Msgs.WORLD.get("i.rate_up");
        rateUp.styleName = "rateUpButton";
        rateUp.group = _radioGroup;
        rateUp.value = true;
        rateUp.useHandCursor = rateUp.buttonMode = true;
        _ratingControls.addChild(rateUp);

        // var rateDown :CommandButton = newButton("rateDownButton", "i.rate_down");
        var rateDown :RadioButton = new RadioButton();
        rateDown.toolTip = Msgs.WORLD.get("i.rate_down");
        rateDown.styleName = "rateDownButton";
        rateDown.group = _radioGroup;
        rateDown.value = false;
        rateDown.useHandCursor = rateDown.buttonMode = true;
        _ratingControls.addChild(rateDown);

        addChild(_ratingControls);
        addChild(FlexUtil.createSpacer(10));

        var showMusic :CommandButton = new CommandButton();
        showMusic.toolTip = Msgs.WORLD.get("i.dj_music");
        showMusic.styleName = "djMusicButton";
        showMusic.setCommand(WorldController.SHOW_MUSIC, showMusic);
        addChild(showMusic);

        onTrackChanged(_roomObj.track);
        onRatingChanged(_roomObj.trackRating);
    }

    protected function onMessageReceived (name :String, args :Array) :void
    {
        switch (name) {
        case RoomObject.TRACK_SKIPPED_MESSAGE:
            _skippedLabel.visible = true;
            _ratingControls.visible = false;

            _skippedLabel.alpha = 1;
            Tweener.addTween(_skippedLabel, {
                time: 1, alpha: 0, onComplete: function () :void {
                    _skippedLabel.visible = false;
                    _ratingControls.visible = true;
                }
            });

            break;
        }
    }

    protected function onRatingChanged (rating :int) :void
    {
        _ratingLabel.visible = true;
        _ratingLabel.text = (rating > 0 ? "+" : "") + rating;

        var o :Object = { size: 26 };
        var updateStyle :Function = function () :void {
            _ratingLabel.setStyle("fontSize", o.size);
        };

        // Grow...
        updateStyle();
        Tweener.addTween(o, {
            time: 0.2, size: 30, onUpdate: updateStyle,
            onComplete: function () :void {
                // And shrink back...
                Tweener.addTween(o, {
                    time: 0.2, size: 26, onUpdate: updateStyle
                });
            }
        });
    }

    protected function onTrackChanged (newTrack :Track, oldTrack :Track = null) :void
    {
        _ratingLabel.visible = false;
        _radioGroup.selection = null;

        if (newTrack == null) {
            // Ok, party's over
            playDefaultStateFrom(/\b(dj|dance)\b/i);

        } else if (amDj(newTrack)) {
            // Started DJ-ing, enter a DJ state (or dance if they don't have one)
            playState([ /\bdj\b/i, /\bdance\b/i ]);
            _radioGroup.enabled = false;

        } else if (amDj(oldTrack)) {
            // Stopped DJ-ing, go back to just dancing
            playState([ /\bdance\b/i ]);
            _radioGroup.enabled = true;
        }
    }

    protected function rateTrack (like :Boolean) :void
    {
        if (amDj(_roomObj.track)) {
            return; // You can't rate your own track
        }

        if (like) {
            // Enter a new dance state
            playState([ /\bdance\b/i ]);
        } else {
            // Stop dancing
            playDefaultStateFrom(/\bdance\b/i);
        }

        _roomObj.roomService.rateTrack(_roomObj.track.audio.itemId, like);
    }

    protected function amDj (track :Track) :Boolean
    {
        return track != null && track.audio.ownerId == _ctx.getMyId();
    }

    protected function playState (patterns :Array) :void
    {
        var avatar :MemberSprite = RoomView(_ctx.getPlaceView()).getMyAvatar();
        if (avatar == null) {
            return;
        }

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

        if (currentState == null || currentState.match(pattern)) {
            var registeredStates :Array = avatar.getAvatarStates();
            if (registeredStates.length > 0) {
                avatar.setState(registeredStates[0]);
            }
        }
    }

    protected var _ctx :MsoyContext;
    protected var _roomObj :RoomObject;

    protected var _radioGroup :RadioButtonGroup;
    protected var _ratingLabel :Label;
    protected var _skippedLabel :Label;
    protected var _ratingControls :HBox;
}
}
