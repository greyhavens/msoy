//
// $Id#

package com.threerings.msoy.ui {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.InteractiveObject;
import flash.display.MovieClip;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import caurina.transitions.Tweener;

import com.threerings.util.Log;
import com.threerings.util.MultiLoader;
import com.threerings.util.ValueEvent;

import com.threerings.flash.DisplayUtil;
import com.threerings.flash.TextFieldUtil;
import com.threerings.flash.video.FlvVideoPlayer;
import com.threerings.flash.video.VideoPlayer;
import com.threerings.flash.video.VideoPlayerCodes;

/**
 * The msoy-skinned video display.
 */
// NOTES:
// - Do we want to allow the youtube watermark to be clickable? - NO
public class MsoyVideoDisplay extends Sprite
{
    public static const WIDTH :int = 320;

    public static const HEIGHT :int = 240;

    public static const DEBUG :Boolean = false;

    /**
     * Create.
     */
    public function MsoyVideoDisplay (player :VideoPlayer, commentCallback :Function = null)
    {
        _player = player;
        _player.addEventListener(VideoPlayerCodes.STATE, handlePlayerState);
        _player.addEventListener(VideoPlayerCodes.SIZE, handlePlayerSize);
        _player.addEventListener(VideoPlayerCodes.DURATION, handlePlayerDuration);
        _player.addEventListener(VideoPlayerCodes.POSITION, handlePlayerPosition);
        _player.addEventListener(VideoPlayerCodes.ERROR, handlePlayerError);
        _commentCallback = commentCallback;

        addChild(_player.getDisplay());

        addEventListener(Event.ADDED_TO_STAGE, handleAddedToStage);

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

    /**
     * Stop playing our video.
     */
    public function unload () :void
    {
        _player.unload();
    }

    protected function configureUI () :void
    {
        // create the mask...
        var masker :Shape = new Shape();
        this.mask = masker;
        addChild(masker);

        var g :Graphics = masker.graphics;
        g.clear();
        g.beginFill(0xFFFFFF);
        g.drawRect(0, 0, WIDTH, HEIGHT);
        g.endFill();

        _hud = new Sprite();
        g = _hud.graphics;
        g.beginFill(0x000000, .85);
        g.drawRect(0, 0, WIDTH, UNIT);
        g.endFill();
        g.lineStyle(1, 0x3db8eb);
        g.drawRect(0, 0, WIDTH - 1, UNIT - 1);
        _hud.y = HEIGHT + 1; // position it offscreenz
        addChild(_hud);

        MultiLoader.getContents(UI, configureUI2);
    }

    protected function configureUI2 (ui :DisplayObject) :void
    {
        _playBtn = DisplayUtil.findInHierarchy(ui, "playbutton");
        _pauseBtn = DisplayUtil.findInHierarchy(ui, "pausebutton");
        _commentBtn = DisplayUtil.findInHierarchy(ui, "commentbutton");
        _volumeBtn = DisplayUtil.findInHierarchy(ui, "fullvolumebutton");
        _muteBtn = DisplayUtil.findInHierarchy(ui, "mutebutton");
        _track = new Sprite();
        var timeline :DisplayObject = DisplayUtil.findInHierarchy(ui, "timeline");
        timeline.x = 0;
        timeline.y = (UNIT - timeline.height) / 2;
        _track.addChild(timeline);
        _knob = new Sprite();
        _knob.y = timeline.y;
        var knobBtn :DisplayObject = DisplayUtil.findInHierarchy(ui, "sliderknob");
        knobBtn.x = knobBtn.width / -2;
        knobBtn.y = 2 + knobBtn.height / -2; // fiddle fiddle
        _knob.addChild(knobBtn);

        // position the buttons on the _hud
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
        _hud.addChild(_volumeBtn);

        if (_commentCallback != null) {
            baseX -= UNIT;
            _commentBtn.x = baseX + (UNIT - _commentBtn.width) / 2;
            _commentBtn.y = (UNIT - _commentBtn.height) / 2;
            _hud.addChild(_commentBtn);
        }

        _timeField = TextFieldUtil.createField("88:88 / 88:88",
            { autoSize: TextFieldAutoSize.CENTER, selectable: false },
            { color: 0xFFFFFF, font: "_sans", size: 10 });
        _timeField.height = UNIT;
        baseX -= _timeField.width;
        _timeField.x = baseX;
        _timeField.y = (UNIT - _timeField.height) / 2
        _hud.addChild(_timeField);
        updateTime(); // set the strings..

        _track.x = UNIT + PAD;
        _trackWidth = baseX - UNIT - (PAD * 2);
        _track.graphics.beginFill(0xFFFFFF, 0);
        _track.graphics.drawRect(0, 1, _trackWidth, UNIT - 1);
        _track.graphics.endFill();
        timeline.width = _trackWidth;

        addEventListener(MouseEvent.ROLL_OVER, handleMouseRoll);
        addEventListener(MouseEvent.ROLL_OUT, handleMouseRoll);

        _playBtn.addEventListener(MouseEvent.CLICK, handlePlay);
        _pauseBtn.addEventListener(MouseEvent.CLICK, handlePause);

        _commentBtn.addEventListener(MouseEvent.CLICK, handleComment);
        _volumeBtn.addEventListener(MouseEvent.CLICK, handleVolume);
        _muteBtn.addEventListener(MouseEvent.CLICK, handleMute);

        _track.addEventListener(MouseEvent.CLICK, handleTrackClick);
        _knob.addEventListener(MouseEvent.MOUSE_DOWN, handleKnobDown);

        effectPlayerVolume(_player.getVolume());
        
        // finally, make sure things are as they should be
        displayPlayState(_player.getState());
        handleAddedToStage(); // will be ok if we're not..
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
        _hud.removeChild(_volumeBtn);
        _hud.addChild(_muteBtn);
        _player.setVolume(0);
    }

    protected function handleMute (event :MouseEvent) :void
    {
        _hud.removeChild(_muteBtn);
        _hud.addChild(_volumeBtn);
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
        _knob.startDrag(false, new Rectangle(0, _knob.y, _trackWidth, 0));
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
        _playing = (state == VideoPlayerCodes.STATE_PLAYING);
        updateHUD();

        if (_playBtn == null) {
            return; // not yet set up.
        }

        var on :DisplayObject;
        var off :DisplayObject;

        if (state == VideoPlayerCodes.STATE_READY || state == VideoPlayerCodes.STATE_PAUSED) {
            on = _playBtn;
            off = _pauseBtn;

        } else if (state == VideoPlayerCodes.STATE_PLAYING) {
            on = _pauseBtn;
            off = _playBtn;

        } else {
            return;
        }

        if (off.parent == _hud) {
            _hud.removeChild(off);
        }
        if (on.parent != _hud) {
            _hud.addChild(on);
        }
    }

    protected function effectPlayerVolume (volume :Number) :void
    {
//        // TODO
//        _volumeBtn.gotoAndStop(0);
    }

    protected function handlePlayerSize (event :ValueEvent) :void
    {
        const size :Point = Point(event.value);

        const disp :DisplayObject = _player.getDisplay();
        // TODO: siggggghhhhh
//        disp.scaleX = NATIVE_WIDTH / size.x;
//        disp.scaleY = NATIVE_HEIGHT / size.y;
        disp.width = WIDTH;
        disp.height = HEIGHT;

        // and, we redispatch
        dispatchEvent(event);
    }

    protected function handlePlayerDuration (event :ValueEvent) :void
    {
        _hud.addChild(_track);
        _durationString = formatTime(Number(event.value));
        updateTime();
    }

    protected function handlePlayerPosition (event :ValueEvent) :void
    {
        updateTime(Number(event.value));

//        trace("Got player position: " + event.value);
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

    protected function handleAddedToStage (event :Event = null) :void
    {
        var mx :Number = this.mouseX;
        var my :Number = this.mouseY;
        _mouseIn = (mx >= 0) && (mx < WIDTH) && (my >= 0) && (my < HEIGHT);
        updateHUD();
    }

    protected function handleMouseRoll (event :MouseEvent) :void
    {
        _mouseIn = (event.type == MouseEvent.ROLL_OVER);
        updateHUD();
    }

    protected function updateHUD () :void
    {
        const show :Boolean = _mouseIn || !_playing || _dragging;
        Tweener.addTween(_hud, {
            time: .25,
            y: show ? HEIGHT - UNIT : HEIGHT + 1,
            transition: "easeinoutcubic" });
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

    protected var _player :VideoPlayer;

    protected var _commentCallback :Function;

    protected var _hud :Sprite;

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
    protected var _mouseIn :Boolean;
    
    protected static const PAD :int = 10;

    /** The unit of size. The width/height of each button & the height of the HUD. */
    protected static const UNIT :int = 28;

    protected static const UNKNOWN_TIME :String = "-:--";

    [Embed(
        source="../../../../../../rsrc/media/skins/videoplayer.swf",
        mimeType="application/octet-stream")]
    protected static const UI :Class;
}
}
