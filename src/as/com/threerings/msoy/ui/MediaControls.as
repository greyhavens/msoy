//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

import com.threerings.flash.SimpleIconButton;
import com.threerings.flash.TextFieldUtil;
import com.threerings.flash.media.MediaPlayer;
import com.threerings.flash.media.MediaPlayerCodes;

public class MediaControls extends Sprite
{
    public static const WIDTH :int = 320;

    public static const HEIGHT :int = UNIT;

    public static const VOLUME_POP_HEIGHT :int = 41;
    public static const VOLUME_TRACK_OFFSET :int = 3;
    public static const VOLUME_TRACK_HEIGHT :int = VOLUME_POP_HEIGHT - (2 * VOLUME_TRACK_OFFSET);
    
    public function MediaControls (player :MediaPlayer, commentCallback :Function = null)
    {
        _player = player;
        _player.addEventListener(MediaPlayerCodes.STATE, handlePlayerState);
        _player.addEventListener(MediaPlayerCodes.DURATION, handlePlayerDuration);
        _player.addEventListener(MediaPlayerCodes.POSITION, handlePlayerPosition);
        _player.addEventListener(MediaPlayerCodes.ERROR, handlePlayerError);
        _commentCallback = commentCallback;

        configureUI();
    }

    override public function get width () :Number
    {
        return WIDTH;
    }

    override public function get height () :Number
    {
        return HEIGHT;
    }

    public function isDraggingSeek () :Boolean
    {
        return _dragging;
    }

    public function unhook () :void
    {
        _player.removeEventListener(MediaPlayerCodes.STATE, handlePlayerState);
        _player.removeEventListener(MediaPlayerCodes.DURATION, handlePlayerDuration);
        _player.removeEventListener(MediaPlayerCodes.POSITION, handlePlayerPosition);
        _player.removeEventListener(MediaPlayerCodes.ERROR, handlePlayerError);
    }

    /**
     * Unload so we can be disposed.
     */
    public function unload () :void
    {
        unhook();
        _player.unload();
    }

    protected function configureUI () :void
    {
        _playBtn = new SimpleIconButton(PLAY_BTN);
        _pauseBtn = new SimpleIconButton(PAUSE_BTN);
        _commentBtn = new SimpleIconButton(COMMENT_BTN);
        _volumeBtn = new SimpleIconButton(calcVolumeIcon(_player.getVolume()));
        _track = new Sprite();
        _knob = new Sprite();
        var knobThing :DisplayObject = DisplayObject(new KNOB());
        _knob.addChild(knobThing);

        // position the buttons
        _playBtn.x = (UNIT - _playBtn.width) / 2;
        _playBtn.y = (UNIT - _playBtn.height) / 2;
        _pauseBtn.x = (UNIT - _pauseBtn.width) / 2;
        _pauseBtn.y = (UNIT - _pauseBtn.height) / 2;
        knobThing.x = knobThing.width / -2;
        knobThing.y = (UNIT - knobThing.height) / 2;

        var baseX :int = WIDTH;

        baseX -= UNIT;
        _volumeBtn.x = baseX + (UNIT - _volumeBtn.width) / 2;
        _volumeBtn.y = (UNIT - _volumeBtn.height) / 2;
        addChild(_volumeBtn);

        if (_commentCallback != null) {
            baseX -= UNIT;
            _commentBtn.x = baseX + (UNIT - _commentBtn.width) / 2;
            _commentBtn.y = (UNIT - _commentBtn.height) / 2;
            addChild(_commentBtn);
        }

        // size and add the timefield
        _timeField = TextFieldUtil.createField("88:88 / 88:88",
            { autoSize: TextFieldAutoSize.CENTER, selectable: false },
            { color: 0xFFFFFF, font: "_sans", size: 10 });
        _timeField.height = UNIT;
        baseX -= _timeField.width;
        _timeField.x = baseX;
        _timeField.y = (UNIT - _timeField.height) / 2
        addChild(_timeField);

        _track.x = UNIT + PAD;
        _trackWidth = baseX - UNIT - (PAD * 2);
        var g :Graphics = _track.graphics;
        g.beginFill(0xFFFFFF, 0);
        g.drawRect(0, 1, _trackWidth, UNIT - 1);
        g.endFill();
        g.lineStyle(1, 0x000000);
        g.beginFill(0xFFFFFF);
        g.drawRect(0, 11, _trackWidth, UNIT - 22)
        g.endFill();
        
        _playBtn.addEventListener(MouseEvent.CLICK, handlePlay);
        _pauseBtn.addEventListener(MouseEvent.CLICK, handlePause);

        _commentBtn.addEventListener(MouseEvent.CLICK, handleComment);
        _volumeBtn.addEventListener(MouseEvent.CLICK, handleVolume);

        _track.addEventListener(MouseEvent.CLICK, handleTrackClick);
        _knob.addEventListener(MouseEvent.MOUSE_DOWN, handleKnobDown);

        // finally, make sure things are as they should be
        displayPlayState(_player.getState());

        updateDuration(_player.getDuration());
        updatePosition(_player.getPosition());
    }

    protected function handlePlay (event :MouseEvent) :void
    {   
        event.stopImmediatePropagation();
        _player.play();
    }

    protected function handlePause (event :MouseEvent) :void
    {
        event.stopImmediatePropagation();
        _player.pause();
    }

    protected function handleComment (event :MouseEvent) :void
    {
        _commentCallback();
    }   

    protected function handleVolume (event :MouseEvent) :void
    {
        if (_volumePop != null) {
            popVolumeDown();
            return;
        }

        _volumePop = new Sprite();
        var g :Graphics = _volumePop.graphics;
        g.beginFill(0x000000, .85);
        g.drawRect(0, 0, UNIT, VOLUME_POP_HEIGHT);
        g.endFill();
        g.lineStyle(1, 0x3db8eb);
        g.drawRect(0, 0, UNIT, VOLUME_POP_HEIGHT);
        g.drawRect(UNIT/2 - 1, VOLUME_TRACK_OFFSET, 2, VOLUME_TRACK_HEIGHT);

        _volumeKnob = new Sprite();
        _volumeKnob.buttonMode = true;
        _volumeKnob.useHandCursor = true;
        g = _volumeKnob.graphics;
        g.beginFill(0xFFFFFF);
        g.drawRect(UNIT/-2 + 3, -2, UNIT - 6, 4);
        g.endFill();
        _volumePop.addChild(_volumeKnob);

        // set to current volume
        _volumeKnob.x = UNIT/2;
        _volumeKnob.y = VOLUME_TRACK_OFFSET + ((1 - _player.getVolume()) * VOLUME_TRACK_HEIGHT);

        _volumePop.addEventListener(MouseEvent.MOUSE_DOWN, handleVolumeTrackClick);
        _volumeKnob.addEventListener(MouseEvent.MOUSE_DOWN, handleVolumeKnobDown);
        _volumePop.x = _volumeBtn.x;
        _volumePop.y = _volumeBtn.y - VOLUME_POP_HEIGHT;
        addChild(_volumePop);
        addEventListener(MouseEvent.ROLL_OUT, handleVolumeRollout);
    }

    protected function popVolumeDown () :void
    {
        removeEventListener(MouseEvent.ROLL_OUT, handleVolumeRollout);
        // pop the fucker down
        removeChild(_volumePop);
        _volumePop = null;
        _volumeKnob = null;
    }

    protected function handleVolumeRollout (event :MouseEvent) :void
    {
        handleVolumeKnobUp(event);
        popVolumeDown();
    }

    protected function handleVolumeKnobDown (event :MouseEvent) :void
    {
        event.stopImmediatePropagation();
        _volumeKnob.startDrag(true,
            new Rectangle(UNIT/2, VOLUME_TRACK_OFFSET, 0, VOLUME_TRACK_HEIGHT));
        addEventListener(MouseEvent.MOUSE_UP, handleVolumeKnobUp);
        addEventListener(MouseEvent.MOUSE_MOVE, handleVolumeAdjust);
    }

    protected function handleVolumeTrackClick (event :MouseEvent) :void
    {
        _volumeKnob.y = event.localY;
        handleVolumeAdjust(event);
    }

    protected function handleVolumeAdjust (event :MouseEvent) :void
    {
        var volume :Number = 1 - ((_volumeKnob.y - VOLUME_TRACK_OFFSET) / VOLUME_TRACK_HEIGHT);
        _player.setVolume(volume);
        _volumeBtn.setIcon(calcVolumeIcon(volume));
    }

    protected function calcVolumeIcon (volume :Number) :Object
    {
        // if the level is 0, show icon 0, else smoothly between 1 and 4
        const icon :int = (volume == 0) ? 0 : (1 + Math.round(volume * 3));
        return VOLUME_ICONS[icon];
    }

    protected function handleVolumeKnobUp (event :MouseEvent) :void
    {
        _volumeKnob.stopDrag();
        removeEventListener(MouseEvent.MOUSE_UP, handleVolumeKnobUp);
        removeEventListener(MouseEvent.MOUSE_MOVE, handleVolumeAdjust);
    }

    protected function handleTrackClick (event :MouseEvent) :void
    {
        event.stopImmediatePropagation();
            
        // we add the listener on the track, but apparently the localX refers to the thing
        // actually clicked. Mother of pearl.
        var p :Point = new Point(event.stageX, event.stageY);
        p = _track.globalToLocal(p);

        adjustSeek(p.x);
    }       
            
    protected function handleKnobDown (event :MouseEvent) :void
    {   
        event.stopImmediatePropagation();
        
        _dragging = true;
        _knob.startDrag(true, new Rectangle(0, _knob.y, _trackWidth, 0));
        addEventListener(Event.ENTER_FRAME, handleKnobSeekCheck);
        addEventListener(MouseEvent.MOUSE_UP, handleKnobUp);
    }   
        
    protected function handleKnobUp (event :MouseEvent) :void
    {   
        event.stopImmediatePropagation();
        endDrag();
    }

    protected function endDrag () :void
    {
        _dragging = false;
        _knob.stopDrag();
        removeEventListener(Event.ENTER_FRAME, handleKnobSeekCheck);
        removeEventListener(MouseEvent.MOUSE_UP, handleKnobUp);
        handleKnobSeekCheck(null);
    }
        
    protected function handleKnobSeekCheck (event :Event) :void
    {   
        adjustSeek(_knob.x);
    }   
        
    protected function handlePlayerState (event :ValueEvent) :void
    {   
        displayPlayState(int(event.value));
    }

    protected function displayPlayState (state :int) :void
    {
        if (_playBtn == null) {
            return; // not yet set up.
        }

        var on :DisplayObject;
        var off :DisplayObject;

        if (state == MediaPlayerCodes.STATE_READY || state == MediaPlayerCodes.STATE_PAUSED) {
            on = _playBtn;
            off = _pauseBtn;

        } else if (state == MediaPlayerCodes.STATE_PLAYING) {
            on = _pauseBtn;
            off = _playBtn;

        } else {
            return;
        }

        if (off.parent == this) {
            removeChild(off);
        }
        if (on.parent != this) {
            addChild(on);
        }
    }

    protected function handlePlayerDuration (event :ValueEvent) :void
    {
        updateDuration(Number(event.value));
    }

    protected function handlePlayerPosition (event :ValueEvent) :void
    {
        updatePosition(Number(event.value));
    }

    protected function handlePlayerError (event :ValueEvent) :void
    {
        // TODO.. maybe just redispatch
        log.warning("player error: " + event.value);
    }

    protected function updateDuration (duration :Number) :void
    {
        _duration = duration;
        ensureParent(isNaN(duration), this, _track);
        _durationString = formatTime(duration);
        updateTime(_player.getPosition());
    }

    protected function updatePosition (pos :Number) :void
    {
        updateTime(pos);

        if (_dragging) {
            if (isNaN(pos)) {
                endDrag();
            } else {
                return;
            }
        }
        _lastKnobX = int.MIN_VALUE;
        _knob.x = (pos / _duration) * _trackWidth;
        ensureParent(isNaN(pos), _track, _knob);
    }

    protected function ensureParent (
        off :Boolean, parent :DisplayObjectContainer, child :DisplayObject) :void
    {
        if (off == (child.parent == parent)) {
            if (off) {
                parent.removeChild(child);
            } else {
                parent.addChild(child);
            }
        }
    }

    protected function adjustSeek (trackX :Number) :void
    {
        // see if we can seek to a position
        const dur :Number = _player.getDuration();
        if (isNaN(dur)) {
            log.debug("Duration is NaN, not seeking.");
            return;
        }
        if (trackX == _lastKnobX) {
            return;
        }
        _lastKnobX = trackX;
        var perc :Number = trackX / _trackWidth;
        perc = Math.max(0, Math.min(1, perc));
        //log.debug("Seek", "x", trackX, "perc", perc, "pos", (perc * dur));
        _player.seek(perc * dur);
    }

    protected function updateTime (position :Number) :void
    {
        _timeField.text = formatTime(position) + " / " + _durationString;
    }

    protected function formatTime (time :Number) :String
    {
        if (isNaN(time)) {
            return UNKNOWN_TIME;
        }
        const mins :int = int(time / 60);
        time -= mins * 60;
        var secString :String = String(Math.round(time));
        if (secString.length == 1) {
            secString = "0" + secString;
        }
        return String(mins) + ":" + secString;
    }

    protected const log :Log = Log.getLog(this);

    protected var _player :MediaPlayer;

    protected var _commentCallback :Function;

    protected var _playBtn :DisplayObject;
    protected var _pauseBtn :DisplayObject;
    protected var _track :Sprite;
    protected var _knob :Sprite;
    protected var _timeField :TextField;
    protected var _commentBtn :SimpleIconButton;
    protected var _volumeBtn :SimpleIconButton;

    protected var _duration :Number = NaN;
    protected var _durationString :String = UNKNOWN_TIME;

    protected var _volumeKnob :Sprite;
    protected var _volumePop :Sprite;

    protected var _trackWidth :Number;

    protected var _lastKnobX :int = int.MIN_VALUE;

    protected var _dragging :Boolean;

    protected var _playing :Boolean;

    protected static const PAD :int = 10;

    protected static const UNIT :int = 28;

    protected static const UNKNOWN_TIME :String = "-:--";

    [Embed(source="../../../../../../rsrc/media/skins/mediaplayer/play.png")]
    protected static const PLAY_BTN :Class;

    [Embed(source="../../../../../../rsrc/media/skins/mediaplayer/pause.png")]
    protected static const PAUSE_BTN :Class;

    // TODO: re-use resource in css file, or have that use this. Or something!
    [Embed(source="../../../../../../rsrc/media/skins/controlbar/comment.png")]
    protected static const COMMENT_BTN :Class;

    // TODO: re-use resource in css file, or have that use this. Or something!
    [Embed(source="../../../../../../rsrc/media/skins/controlbar/vol_05.png")]
    protected static const VOLUME_4 :Class;

    // TODO: re-use resource in css file, or have that use this. Or something!
    [Embed(source="../../../../../../rsrc/media/skins/controlbar/vol_04.png")]
    protected static const VOLUME_3 :Class;

    // TODO: re-use resource in css file, or have that use this. Or something!
    [Embed(source="../../../../../../rsrc/media/skins/controlbar/vol_03.png")]
    protected static const VOLUME_2 :Class;

    // TODO: re-use resource in css file, or have that use this. Or something!
    [Embed(source="../../../../../../rsrc/media/skins/controlbar/vol_02.png")]
    protected static const VOLUME_1 :Class;

    // TODO: re-use resource in css file, or have that use this. Or something!
    [Embed(source="../../../../../../rsrc/media/skins/controlbar/vol_01.png")]
    protected static const VOLUME_0 :Class;

    protected static const VOLUME_ICONS :Array = [
        VOLUME_0, VOLUME_1, VOLUME_2, VOLUME_3, VOLUME_4 ];

    [Embed(source="../../../../../../rsrc/media/skins/mediaplayer/knob.swf")]
    protected static const KNOB :Class;
}
}
