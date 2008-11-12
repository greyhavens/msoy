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
        g.lineStyle(1, 0x3db8eb);
        g.drawRect(0, 0, WIDTH, BUTTON_DIM);
        g.endFill();
        _hud.y = HEIGHT + 1; // position it offscreenz
        addChild(_hud);

        MultiLoader.getContents(UI, configureUI2);
    }

    protected function configureUI2 (ui :DisplayObject) :void
    {
//        // TEMP
//        trace(DisplayUtil.dumpHierarchy(ui));

        _playBtn = DisplayUtil.findInHierarchy(ui, "playbutton");
        _pauseBtn = DisplayUtil.findInHierarchy(ui, "pausebutton");
        _track = new Sprite();
        _track.graphics.beginFill(0xFFFFFF);
        _track.graphics.drawRect(0, 0, 1, 5);
        _track.graphics.endFill();
        // TEMP trackthing hacking
//        var trackThing :DisplayObject = DisplayUtil.findInHierarchy(ui, "timeline");
//        trackThing.x = trackThing.width / 2 + IDIOT_OFFSET;
//        trackThing.y = 0;
//        _track.addChild(trackThing);
        _knob = new Sprite();
        _knob.addChild(DisplayUtil.findInHierarchy(ui, "sliderknob"));
        _commentBtn = DisplayUtil.findInHierarchy(ui, "commentbutton");
        _volumeBtn = DisplayUtil.findInHierarchy(ui, "volume") as MovieClip;

//        trace("============= playBtn dimensions: " + _playBtn.width + ", " + _playBtn.height);
//        trace("============= pauseBtn dimensions: " + _pauseBtn.width + ", " + _pauseBtn.height);
//        trace("============= comment dimensions: " + _commentBtn.width + ", " + _commentBtn.height);
//        trace("============= volume dimensions: " + _volumeBtn.width + ", " + _volumeBtn.height);
//        trace("playBtnx/y: " + _playBtn.x + ", " + _playBtn.y);
//        trace("=== total frames: " + _volumeBtn.totalFrames);

        // position the buttons on the _hud
        _playBtn.x = (BUTTON_DIM - _playBtn.width) / 2;
        _playBtn.y = (BUTTON_DIM - _playBtn.height) / 2;

        _pauseBtn.x = (BUTTON_DIM - _pauseBtn.width) / 2;
        _pauseBtn.y = (BUTTON_DIM - _pauseBtn.height) / 2;

        var hudG :Graphics = _hud.graphics;
        hudG.lineStyle(1, 0xFF0000);

        var baseX :int = WIDTH;

        baseX -= BUTTON_DIM;
        if (DEBUG) {
            hudG.moveTo(baseX, 0);
            hudG.lineTo(baseX, BUTTON_DIM);
        }
        _volumeBtn.x = baseX + (BUTTON_DIM - _volumeBtn.width) / 2;
        _volumeBtn.y = (BUTTON_DIM - _volumeBtn.height) / 2;
        _hud.addChild(_volumeBtn);

        if (_commentCallback != null) {
            baseX -= BUTTON_DIM;
            if (DEBUG) {
                hudG.moveTo(baseX, 0);
                hudG.lineTo(baseX, BUTTON_DIM);
            }
            _commentBtn.x = baseX + (BUTTON_DIM - _commentBtn.width) / 2;
            _commentBtn.y = (BUTTON_DIM - _commentBtn.height) / 2;
            _hud.addChild(_commentBtn);
        }

        _timeField = TextFieldUtil.createField("88:88 / 88:88",
            { autoSize: TextFieldAutoSize.CENTER, selectable: false },
            { color: 0xFFFFFF, font: "_sans", size: 10 });
        _timeField.height = BUTTON_DIM;
        baseX -= _timeField.width;
        if (DEBUG) {
            hudG.moveTo(baseX, 0);
            hudG.lineTo(baseX, BUTTON_DIM);
        }
        _timeField.x = baseX;
        _timeField.y = (BUTTON_DIM - _timeField.height) / 2
        _hud.addChild(_timeField);
        updateTime(); // set the strings..

        _track.x = BUTTON_DIM + PAD;
        _track.y = BUTTON_DIM / 2;
        _track.width = baseX - PAD - _track.x;

        // TEMP
        _playBtn.x += IDIOT_OFFSET;
        _playBtn.y += IDIOT_OFFSET;
        _pauseBtn.x += IDIOT_OFFSET;
        _pauseBtn.y += IDIOT_OFFSET;
        _commentBtn.x += IDIOT_OFFSET;
        _commentBtn.y += IDIOT_OFFSET;
        _volumeBtn.x += IDIOT_OFFSET;
        _volumeBtn.y += IDIOT_OFFSET;

        addEventListener(MouseEvent.ROLL_OVER, handleMouseRoll);
        addEventListener(MouseEvent.ROLL_OUT, handleMouseRoll);

        _playBtn.addEventListener(MouseEvent.CLICK, handlePlay);
        _pauseBtn.addEventListener(MouseEvent.CLICK, handlePause);

        _commentBtn.addEventListener(MouseEvent.CLICK, handleComment);
        _volumeBtn.addEventListener(MouseEvent.CLICK, handleVolume);

        _track.addEventListener(MouseEvent.CLICK, handleTrackClick);
        _knob.addEventListener(MouseEvent.MOUSE_DOWN, handleKnobDown);

        effectPlayerVolume(_player.getVolume());
        
        // finally, make sure things are as they should be
        displayPlayState(_player.getState());
        handleAddedToStage(); // will be ok if we're not..

//        _track = new Sprite();
//        _track.x = PAD - _hud.x;
//        _track.y = NATIVE_HEIGHT - PAD - TRACK_HEIGHT - _hud.y;
//        g = _track.graphics;
//        g.beginFill(0x000000, .7);
//        g.drawRect(0, 0, TRACK_WIDTH, TRACK_HEIGHT);
//        g.endFill();
//        g.lineStyle(2, 0xFFFFFF);
//        g.drawRect(0, 0, TRACK_WIDTH, TRACK_HEIGHT);
//        // _track is not added to _hud until we know the duration
//
//        const trackMask :Shape = new Shape();
//        g = trackMask.graphics;
//        g.beginFill(0xFFFFFF);
//        g.drawRect(0, 0, TRACK_WIDTH, TRACK_HEIGHT);
//        _track.addChild(trackMask);
//        _track.mask = trackMask;
//
//        _knob = new Sprite();
//        _knob.y = TRACK_HEIGHT / 2;
//        g = _knob.graphics;
//        g.lineStyle(1, 0xFFFFFF);
//        g.beginFill(0x000099);
//        g.drawCircle(0, 0, TRACK_HEIGHT/2 - 1);
//        // _knob is not added to _track until we know the position
//
//        _track.addEventListener(MouseEvent.CLICK, handleTrackClick);
//        _knob.addEventListener(MouseEvent.MOUSE_DOWN, handleKnobDown);
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
        // fucking goddamn fucking frames are 1-based, not 0-based
        var frame :int = _volumeBtn.currentFrame - 1; // subtract 1
        frame = (frame + 1) % _volumeBtn.totalFrames; // do the increment
        _volumeBtn.gotoAndStop(frame + 1); // re-add 1
        trace("===== volume clicked");
    }

    protected function handleTrackClick (event :MouseEvent) :void
    {
        event.stopImmediatePropagation();

        var p :Point = new Point(event.stageX, event.stageY);
        p = _track.globalToLocal(p);

        adjustSeek(p.x);
    }

    protected function handleKnobDown (event :MouseEvent) :void
    {
        event.stopImmediatePropagation();

        _dragging = true;
        _knob.startDrag(false, new Rectangle(0, 0, _track.width, 0));
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
        // TODO
        _volumeBtn.gotoAndStop(0);
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
        _knob.x = (pos / _player.getDuration()) * _track.width;
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
        var perc :Number = trackX / _track.width;
        perc = Math.max(0, Math.min(1, perc));
        log.debug("Seek", "x", trackX, "perc", perc, "pos", (perc * dur));
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
        const show :Boolean = _mouseIn || !_playing;
        Tweener.addTween(_hud, {
            time: .25,
            y: show ? HEIGHT - BUTTON_DIM : HEIGHT + 1,
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
    protected var _volumeBtn :MovieClip;

    protected var _durationString :String = UNKNOWN_TIME;

    protected var _lastKnobX :int = int.MIN_VALUE;

    protected var _dragging :Boolean;

    protected var _playing :Boolean;
    protected var _mouseIn :Boolean;
    
    protected static const PAD :int = 10;

    protected static const BUTTON_DIM :int = 28;

    // TEMP
    protected static const IDIOT_OFFSET :int = 12;

    protected static const UNKNOWN_TIME :String = "-:--";

    [Embed(
        source="../../../../../../rsrc/media/skins/videoplayer.swf",
        mimeType="application/octet-stream")]
    protected static const UI :Class;
}
}
