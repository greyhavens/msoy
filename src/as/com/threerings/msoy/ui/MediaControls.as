//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObject;
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
import com.threerings.util.MultiLoader;
import com.threerings.util.ValueEvent;

import com.threerings.flash.DisplayUtil;
import com.threerings.flash.TextFieldUtil;
import com.threerings.flash.media.MediaPlayer;
import com.threerings.flash.media.MediaPlayerCodes;

public class MediaControls extends Sprite
{
    public static const WIDTH :int = 320;

    public static const HEIGHT :int = UNIT;
    
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

    /**
     * Unload so we can be disposed.
     */
    public function unload () :void
    {
        _player.unload();
    }

    protected function configureUI () :void
    {
        // we need to set some things up immediately, as they'll be adjusted when we get
        // events from the player, which may happen prior to the multiloader loading the UI bits
        _track = new Sprite();
        _knob = new Sprite();
        _timeField = TextFieldUtil.createField("",
            { autoSize: TextFieldAutoSize.CENTER, selectable: false },
            { color: 0xFFFFFF, font: "_sans", size: 10 });

        MultiLoader.getContents(UI, configureUI2);
    }

    protected function configureUI2 (ui :DisplayObject) :void
    {
        _playBtn = DisplayUtil.findInHierarchy(ui, "playbutton");
        _pauseBtn = DisplayUtil.findInHierarchy(ui, "pausebutton");
        _commentBtn = DisplayUtil.findInHierarchy(ui, "commentbutton");
        _volumeBtn = DisplayUtil.findInHierarchy(ui, "fullvolumebutton");
        _muteBtn = DisplayUtil.findInHierarchy(ui, "mutebutton");
        var timeline :DisplayObject = DisplayUtil.findInHierarchy(ui, "timeline");
        timeline.x = 0;
        timeline.y = (UNIT - timeline.height) / 2;
        _track.addChildAt(timeline, 0);
        _knob.y = timeline.y;
        var knobBtn :DisplayObject = DisplayUtil.findInHierarchy(ui, "sliderknob");
        knobBtn.x = knobBtn.width / -2;
        knobBtn.y = 2 + knobBtn.height / -2; // fiddle fiddle
        _knob.addChild(knobBtn);

        // position the buttons
        _playBtn.x = (UNIT - _playBtn.width) / 2;
        _playBtn.y = (UNIT - _playBtn.height) / 2;
        _pauseBtn.x = (UNIT - _pauseBtn.width) / 2;
        _pauseBtn.y = (UNIT - _pauseBtn.height) / 2;

        var baseX :int = WIDTH;

        baseX -= UNIT;
        _volumeBtn.x = baseX + (UNIT - _volumeBtn.width) / 2;
        _volumeBtn.y = (UNIT - _volumeBtn.height) / 2;
        _muteBtn.x = baseX + (UNIT - _muteBtn.width) / 2;
        _muteBtn.y = (UNIT - _volumeBtn.height) / 2;
        addChild(_volumeBtn);

        if (_commentCallback != null) {
            baseX -= UNIT;
            _commentBtn.x = baseX + (UNIT - _commentBtn.width) / 2;
            _commentBtn.y = (UNIT - _commentBtn.height) / 2;
            addChild(_commentBtn);
        }

        // size and add the timefield
        TextFieldUtil.updateText(_timeField, "88:88 / 88:88");
        _timeField.height = UNIT;
        baseX -= _timeField.width;
        _timeField.x = baseX;
        _timeField.y = (UNIT - _timeField.height) / 2
        addChild(_timeField);
        updateTime(); // set the strings..

        _track.x = UNIT + PAD;
        _trackWidth = baseX - UNIT - (PAD * 2);
        _track.graphics.beginFill(0xFFFFFF, 0);
        _track.graphics.drawRect(0, 1, _trackWidth, UNIT - 1);
        _track.graphics.endFill();
        timeline.width = _trackWidth;

        _playBtn.addEventListener(MouseEvent.CLICK, handlePlay);
        _pauseBtn.addEventListener(MouseEvent.CLICK, handlePause);

        _commentBtn.addEventListener(MouseEvent.CLICK, handleComment);
        _volumeBtn.addEventListener(MouseEvent.CLICK, handleVolume);
        _muteBtn.addEventListener(MouseEvent.CLICK, handleMute);

        _track.addEventListener(MouseEvent.CLICK, handleTrackClick);
        _knob.addEventListener(MouseEvent.MOUSE_DOWN, handleKnobDown);

        // finally, make sure things are as they should be
        displayPlayState(_player.getState());
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
        removeChild(_volumeBtn);
        addChild(_muteBtn);
        _player.setVolume(0); 
    }   

    protected function handleMute (event :MouseEvent) :void
    {
        removeChild(_muteBtn);
        addChild(_volumeBtn);
        _player.setVolume(1);
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
        if (_track.parent != this) {
            addChild(_track);
        }
        _durationString = formatTime(Number(event.value));
        updateTime();
    }

    protected function handlePlayerPosition (event :ValueEvent) :void
    {
        updateTime(Number(event.value));

        if (_dragging) {
            return;
        }
        _lastKnobX = int.MIN_VALUE;
        const pos :Number = Number(event.value);
        _knob.x = (pos / _player.getDuration()) * _trackWidth;
        if (_knob.parent == null) {
            _track.addChild(_knob);
        }
    }

    protected function handlePlayerError (event :ValueEvent) :void
    {
        // TODO.. maybe just redispatch
        log.warning("player error: " + event.value);
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

    protected function updateTime (position :Number = NaN) :void
    {
        var posString :String = isNaN(position) ? UNKNOWN_TIME : formatTime(position);

        _timeField.text = posString + " / " + _durationString;
    }

    protected function formatTime (time :Number) :String
    {
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
    protected var _commentBtn :DisplayObject;
    protected var _volumeBtn :DisplayObject;
    protected var _muteBtn :DisplayObject;

    protected var _durationString :String = UNKNOWN_TIME;

    protected var _trackWidth :Number;

    protected var _lastKnobX :int = int.MIN_VALUE;

    protected var _dragging :Boolean;

    protected var _playing :Boolean;

    protected static const PAD :int = 10;

    protected static const UNIT :int = 28;

    protected static const UNKNOWN_TIME :String = "-:--";

    [Embed(
        source="../../../../../../rsrc/media/skins/mediaplayer.swf",
        mimeType="application/octet-stream")]
    protected static const UI :Class;
}
}
