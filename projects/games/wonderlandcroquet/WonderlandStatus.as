package {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.events.Event;

import com.threerings.util.EmbeddedSwfLoader;
import com.threerings.util.EmbeddedSwfLoader;

import com.threerings.ezgame.EZGameControl;

public class WonderlandStatus extends Sprite
{
    /** Our game control object. */
    public var gameCtrl :EZGameControl;

    public function WonderlandStatus (parent :DisplayObject, gameCtrl :EZGameControl)
    {
        _parent = parent;
        this.gameCtrl = gameCtrl;

        _loader = new EmbeddedSwfLoader();
        _loader.addEventListener(Event.COMPLETE, loaderComplete);
        try {
            _loader.load(new STATUS_ART_CLASS());
        } catch (re :ReferenceError) {
            trace("Error making status display: " + re);
        }
    }

    public function targetWicket (playerIdx :int, wicketIdx :int) :void
    {
        if (playerIdx == gameCtrl.seating.getMyPosition() && wicketIdx > 0) {
            _cards[wicketIdx - 1].setTargetted(false);
            _cards[wicketIdx].setTargetted(true);
        }

        // TODO: Move their ball to the correct place

        if (wicketIdx > 0) {
            _cards[wicketIdx - 1].removeBall(_balls[playerIdx]);
        }

        if (_balls[playerIdx] != null) {
            _cards[wicketIdx].addBall(_balls[playerIdx]);
        }
    }

    protected function loaderComplete (evt :Event) :void
    {
        var ii :int;
        for (ii = 0; ii < WonderlandCodes.MAX_PLAYERS; ii ++) {
            _balls[ii] = new (_loader.getClass("marker" + (ii + 1)))();
        }

        var y :int = 0;

        for (ii = 0; ii < WonderlandCodes.WICKET_COUNT; ii ++) {
            _cards[ii] = new StatusCard(new (_loader.getClass("tally" + (ii + 1)))(),
                                        new (_loader.getClass("tallyback"))());
            _cards[ii].y = y;

            y -= 25;

            addChildAt(_cards[ii], 0);
        }

        _cards[0].showFace();
    }

    /** The sprite that we scroll around as needed. */
    protected var _parent :DisplayObject;

    /** Our swf loader. */
    protected var _loader :EmbeddedSwfLoader;

    /** Balls. */
    protected var _balls :Array = [];

    /** Cards. */
    protected var _cards :Array = [];

    /** The wicket # each player is targetting. */
    protected var _targets :Array = [];

    [Embed(source="rsrc/status.swf", mimeType="application/octet-stream")]
    protected static const STATUS_ART_CLASS :Class;
}
}

import flash.display.Sprite;

import flash.display.MovieClip;

class StatusCard extends Sprite
{
    public function StatusCard (front :MovieClip, back :MovieClip)
    {
        _front = front;
        _back = back;

        gotoFrame(1);

        addChild(_back);
    }

    public function gotoFrame (frame :int) :void
    {
        _front.gotoAndStop(frame);
        _back.gotoAndStop(frame);
    }


    public function showFace () :void
    {
        addChildAt(_front, 0);
        removeChild(_back);
    }

    public function addBall (ball :MovieClip) :void
    {
        _balls.push(ball);
        rearrangeBalls();
        addChild(ball);
    }

    public function removeBall (ball :MovieClip) :void
    {
        var ii :int = _balls.indexOf(ball);

        if (ii != -1) {
            removeChild(ball);
            _balls.splice(ii, 1);
            rearrangeBalls();
        }
    }

    protected function rearrangeBalls () :void
    {
        var x :int = -20;
        for (var ii :int = 0; ii < _balls.length; ii++) {
            _balls[ii].x = x;
            _balls[ii].y = -40;

            x += 15;
        }
    }

    public function setTargetted (targetted :Boolean) :void
    {
        if (targetted == true) {
            showFace();
            _front.rotation = -5;
            _front.x = -10;
            gotoFrame(2);

        } else {
            _front.x = 0;
            _front.rotation = 0;
            gotoFrame(1);
        }
    }

    protected var _isTargetted :Boolean = false;

    protected var _front :MovieClip;
    protected var _back :MovieClip;

    /** An array of MovieClips for the balls currently targetting us. */
    protected var _balls :Array = [];
}
