package {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.events.Event;

import com.threerings.util.EmbeddedSwfLoader;

import com.threerings.ezgame.EZGameControl;

/**
 * Our little HUD showing who's turn it is, who's targetting what wicket, etc.
 */
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

    /**
     * Update the display to show that the specified player is now targetting the specified wicket.
     */
    public function targetWicket (playerIdx :int, wicketIdx :int) :void
    {
        if (playerIdx == gameCtrl.seating.getMyPosition() && wicketIdx > 0) {
            _cards[wicketIdx - 1].setTargetted(false);
            _cards[wicketIdx].setTargetted(true);
        }

        if (wicketIdx > 0) {
            _cards[wicketIdx - 1].removeBall(_balls[playerIdx]);
        }

        if (_balls[playerIdx] != null) {
            _cards[wicketIdx].addBall(_balls[playerIdx]);
        }
    }

    /**
     * Does the magic setting things up once the graphics have finished loading.
     */
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

        for (ii = 0; ii < gameCtrl.seating.getPlayerIds().length; ii++) {
            targetWicket(ii, 0);
        }

        var ctrl :Sprite = new Sprite();

        // Pan
        ctrl.addChild(new PanControl(new (_loader.getClass("panup"))(),    _parent, 0, STEP_SIZE));
        ctrl.addChild(new PanControl(new (_loader.getClass("pandown"))(),  _parent, 0, -STEP_SIZE));
        ctrl.addChild(new PanControl(new (_loader.getClass("panleft"))(),  _parent,  STEP_SIZE, 0));
        ctrl.addChild(new PanControl(new (_loader.getClass("panright"))(), _parent, -STEP_SIZE, 0));

        // Zoom
        ctrl.addChild(new ZoomControl(new (_loader.getClass("zoomin"))(),  
                                      _parent,  ZOOM_PERCENT/100));
        ctrl.addChild(new ZoomControl(new (_loader.getClass("zoomout"))(),
                                      _parent, -ZOOM_PERCENT/100));
        _cards[0].addChild(ctrl);
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

    /* How far to move on each click on an arrow. */
    protected static const STEP_SIZE :int = 50;

    /* How many percent to nudge our zoom by. */
    protected static const ZOOM_PERCENT :Number = 2.5;

    [Embed(source="rsrc/status.swf", mimeType="application/octet-stream")]
    protected static const STATUS_ART_CLASS :Class;
}
}

import flash.display.DisplayObject;
import flash.display.MovieClip;
import flash.display.Sprite;

import flash.events.MouseEvent;
import flash.events.Event;

/**
 * The actual cards making up our status display.
 */
class StatusCard extends Sprite
{
    public function StatusCard (front :MovieClip, back :MovieClip)
    {
        _front = front;
        _back = back;

        gotoFrame(1);

        addChild(_back);
    }

    /**
     * Moves the animation for this card to the specified frame.
     */
    public function gotoFrame (frame :int) :void
    {
        _front.gotoAndStop(frame);
        _back.gotoAndStop(frame);
    }

    /**
     * Show the face of this card, rather than its back.
     */
    public function showFace () :void
    {
        // TODO: Do a fancy little animation with sliding the card off the screen, then back on.
        addChildAt(_front, 0);
        removeChild(_back);
    }

    /**
     * Add a marker for a player to this card, indicating they're targetting this wicket.
     */
    public function addBall (ball :MovieClip) :void
    {
        _balls.push(ball);
        rearrangeBalls();
        addChild(ball);
    }

    /**
     * Remove the marker for that player's ball from this card.
     */
    public function removeBall (ball :MovieClip) :void
    {
        var ii :int = _balls.indexOf(ball);

        if (ii != -1) {
            removeChild(ball);
            _balls.splice(ii, 1);
            rearrangeBalls();
        }
    }

    /**
     * Recalculate positions for the balls representing anyone targetting this wicket.
     */
    protected function rearrangeBalls () :void
    {
        var x :int = -35;
        for (var ii :int = 0; ii < _balls.length; ii++) {
            _balls[ii].x = x;
            _balls[ii].y = -45;

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
            // TODO: Animate this motion
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

class ScrollerControl extends Sprite
{
    public function ScrollerControl (art :DisplayObject, parent :DisplayObject)
    {
        _parent = parent;
        addChild(art);
        addEventListener(MouseEvent.MOUSE_DOWN, mouseDown);
    }

    protected function mouseDown (event :MouseEvent) :void
    {
        addEventListener(Event.ENTER_FRAME, enterFrame);
        addEventListener(MouseEvent.MOUSE_UP, mouseUp);

        // And call it once immediately
        enterFrame(null);
    }

    protected function mouseUp (event :MouseEvent) :void
    {
        removeEventListener(MouseEvent.MOUSE_UP, mouseUp);
        removeEventListener(Event.ENTER_FRAME, enterFrame);
    }

    protected function enterFrame (event :Event) :void
    {
        // Nothing by default
    }

    protected var _parent :DisplayObject;
}

class ZoomControl extends ScrollerControl
{
    public function ZoomControl (art :DisplayObject, parent :DisplayObject, zoomPercent :Number)
    {
        super(art, parent);
        _zoomPercent = zoomPercent;
    }

    public static function zoom (parent :DisplayObject, zoomPercent :Number) :void
    {
        // FIXME: We potentially want to zoom about the center of the board, or some such.
        var scale :Number = parent.scaleX + zoomPercent;

        scale = Math.max(0.1,  scale);
        scale = Math.min(2, scale);

        parent.scaleX = parent.scaleY = scale;
    }

    override protected function enterFrame (event :Event) :void
    {
        zoom(_parent, _zoomPercent);
    }

    protected var _zoomPercent :Number;
}

class PanControl extends ScrollerControl
{
    public function PanControl (art :DisplayObject, parent :DisplayObject, dx :int, dy :int)
    {
        super(art, parent);
        _dx = dx;
        _dy = dy;
    }

    override protected function enterFrame (event :Event) :void
    {
        _parent.x += _dx;
        _parent.y += _dy;

        // TODO: lock this to a reasonable border around the parent
    }

    protected var _dx :int;
    protected var _dy :int;
}
