package {

import flash.display.Bitmap;
import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.MouseEvent;
import flash.events.Event;

import flash.geom.Point;

import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;

import mx.core.*;
import mx.utils.ObjectUtil;

import com.whirled.WhirledGameControl;

import org.cove.ape.*;

[SWF(width="1000", height="506")]
public class WonderlandCroquet extends Sprite
    implements PropertyChangedListener, StateChangedListener
{
    /** Hard code my goddamned dimensions for now since I can't touch the stage to do this
     * dynamically with screen resizes.
     */
    public static const WIDTH :int = 1000;
    public static const HEIGHT :int = 506;

    /** Our game control object. */
    public var gameCtrl :WhirledGameControl;

    /** Our map. */
    public var map :WonderlandMap;

    /** The player index for the local player. */
    public var myIdx :int;

    public function WonderlandCroquet ()
    {
        // For now, mask us off to my specific size.
        var mask :Shape = new Shape();
        addChild(mask);
        mask.graphics.clear();
        mask.graphics.beginFill(0xFFFFFF);
        mask.graphics.drawRect(0, 0, WIDTH, HEIGHT);
        mask.graphics.endFill();
        this.mask = mask;

        gameCtrl = new WhirledGameControl(this);
        gameCtrl.registerListener(this);

        _spr = new Sprite();
        _ballLayer = new Sprite();
        addEventListener(Event.ENTER_FRAME, tick);

        addChild(_spr);

        // TODO: support better map loading, choice at table time,
        // or maybe just pick one randomly
        //map = new MapBasic();
        map = new MapFancy();
        _spr.addChild(map.background);
        map.background.addEventListener(MouseEvent.MOUSE_DOWN, mouseDown);

        _spr.addChild(_ballLayer);

        APEngine.init(1/3);
        APEngine.defaultContainer = this;

        for each (var particle :AbstractParticle in map.particles) {
            APEngine.addParticle(particle);
        }

        _spr.addChild(map.foreground);

        _ballMarkerLayer = new Sprite();
        _ballMarkerLayer.mouseChildren = false;
        _ballMarkerLayer.mouseEnabled = false;
        _spr.addChild(_ballMarkerLayer);

        _status = new WonderlandStatus(_spr, this);
        addChild(_status);

        addEventListener(Event.ENTER_FRAME, firstFrameSetup);
    }

    /**
     * Adds a mallet to be displayed.
     */
    public function addMallet (mallet :DisplayObject) :void
    {
        _spr.addChild(mallet);
    }

    /**
     * Removes a mallet to be displayed.
     */
    public function removeMallet (mallet :DisplayObject) :void
    {
        _spr.removeChild(mallet);
    }

    /**
     * Sets up a couple of things that need to wait until the universe has somewhat
     * settled down.
     */
    protected function firstFrameSetup (event :Event) :void
    {
        removeEventListener(Event.ENTER_FRAME, firstFrameSetup);

        /** Argh! I can't touch the stage to find out how big I am!
        stage.addEventListener(flash.events.Event.RESIZE, stageResize);
        */

        positionStatus();
        panTo(map.startPoint.x, map.startPoint.y);
    }

    /**
     * Moves the status display to the bottom right of our current window.
     */
    protected function positionStatus () :void
    {
        /** Argh! I can't touch the stage to find out how big I am!
        _status.x = stage.stageWidth - 50;
        _status.y = stage.stageHeight - 75;
        */

        _status.x = WIDTH - 21;
        _status.y = HEIGHT - 25;
    }

    protected function stageResize (event :Event) :void
    {
        positionStatus();
    }

    protected function mouseDown (event :MouseEvent) :void
    {
        this.addEventListener(MouseEvent.MOUSE_MOVE, mouseMove);
        this.addEventListener(MouseEvent.MOUSE_UP, mouseUp);
        _lastMousePosition.x = event.stageX;
        _lastMousePosition.y = event.stageY;
    }

    protected function mouseMove (event :MouseEvent) :void
    {
        if (!event.buttonDown) {
            // Damnit. They probably dragged outside the flash frame, released the button
            // and then came back in. We'll just ignore where the mouse is now and register that
            // the button came back up
            mouseUp(event);
            return;
        }

        var dx :Number = event.stageX - _lastMousePosition.x;
        var dy :Number = event.stageY - _lastMousePosition.y;
        _lastMousePosition.x = event.stageX;
        _lastMousePosition.y = event.stageY;

        _status.pan(dx / _spr.scaleX, dy / _spr.scaleY);
    }

    protected function mouseUp (event :MouseEvent) :void
    {
        this.removeEventListener(MouseEvent.MOUSE_MOVE, mouseMove);
        this.removeEventListener(MouseEvent.MOUSE_UP, mouseUp);
    }

    /**
     * Add some random balls.
     */
    protected function addRandomBalls () :void
    {
        for (var ii: int = 0; ii < 6; ii++) {
            var r :Number = Math.random() * (100 - Ball.RADIUS);
            var angle :Number = Math.random() * 2 * Math.PI;

            var ball: BallParticle = new BallParticle(
                map.startPoint.x + (Math.cos(angle) * r),
                map.startPoint.y + (Math.sin(angle) * r),
                Ball.RADIUS, ii, false);

            APEngine.addParticle(ball);
            _ballLayer.addChild(ball.ball);
        }
    }

    protected function tick (evt :Event) :void
    {
        var particle :AbstractParticle;
        var particles :Array = APEngine.getAll();

        var doneMoving :Boolean = true;

        for each (particle in particles) {
            if (particle is BallParticle) {
                map.applyModifierForce(BallParticle(particle));
            }
        }

        APEngine.step();

        for each (particle in particles) {
            if (particle is BallParticle) {
                if (BallParticle(particle).tick()) {
                    doneMoving = false;
                }
            }
        }

        // TODO: Once hit testing is properly sorted out so we really know when the ball is
        // hidden, it might look nice to show the marker at any time the ball is obstructed,
        // so we get the little ring shooting under the cards. A quick test of it looked nice.
        if (doneMoving) {
            // Show markers for obstructed balls
            for each (var ball :BallParticle in _balls) {
                if (ball.ball.hitTestObject(map.foreground)) {
                    _ballMarkerLayer.addChild(ball.ball.ballMarker);
                    ball.ball.ballMarker.x = ball.ball.x;
                    ball.ball.ballMarker.y = ball.ball.y;
                }
            }
        } else {
            // pan to keep the current ball in view
            var bp :BallParticle = _balls[_currentPlayerIdx];

            if (bp != null) {
                var dx :Number = bp.ball.x - ( WIDTH/2 - _spr.x);
                var dy :Number = bp.ball.y - ( HEIGHT/2 - _spr.y);

                if ((dx*dx + dy*dy) > AUTO_PAN_THRESHOLD * AUTO_PAN_THRESHOLD) {
                    panTo(bp.ball.x, bp.ball.y);
                }
            }

            // Clear markers for obstructed balls
            for (var ii :int = 0; ii < _ballMarkerLayer.numChildren; ii++) {
                _ballMarkerLayer.removeChildAt(0);
            }
        }

        if (_haveMoved && doneMoving && gameCtrl.isMyTurn()) {
            if (_moveAgain) {
                gameCtrl.sendChat(gameCtrl.getOccupantName(gameCtrl.getTurnHolder()) + 
                                  " passed a wicket, and gets an extra swing.");
                startTurn();
            } else {
                gameCtrl.endTurn();
            }
        }
    }

    public function getBallParticle (playerIdx :int) :BallParticle
    {
        return _balls[playerIdx];
    }

    // from StateChangedListener
    public function stateChanged (event :StateChangedEvent) :void
    {
        if (event.type == StateChangedEvent.TURN_CHANGED) {
            var firstTurn :Boolean = false;

            if (_balls == null) {
                firstTurn = true;
                _balls = [];
                _wickets = [];
            }

            _currentPlayerIdx = gameCtrl.seating.getPlayerPosition(gameCtrl.getTurnHolder());

            if (gameCtrl.isMyTurn()) {
                if (firstTurn) {
                    // FIXME: I'm not quite happy with this, but if I just set it, it doesn't appear
                    // to have taken effect by the time it's my turn and I need to actually add a
                    // ball
                    gameCtrl.setImmediate("balls", _balls);
                    gameCtrl.setImmediate("wickets", _wickets);
                }
                gameCtrl.localChat("Your turn!");
                startTurn();
            } else {
                gameCtrl.localChat(gameCtrl.getOccupantName(gameCtrl.getTurnHolder()) + "'s turn");
            }

            if (_balls[_currentPlayerIdx] != null) {
                panTo(_balls[_currentPlayerIdx].ball.x, _balls[_currentPlayerIdx].ball.y);
            }

            _status.turnChanged(_currentPlayerIdx);
        } else if (event.type == StateChangedEvent.GAME_STARTED) {
            gameCtrl.localChat("Wonderland Croquet!");

            myIdx = gameCtrl.seating.getMyPosition();

        } else if (event.type == StateChangedEvent.GAME_ENDED) {
            gameCtrl.localChat("Off with your head!");

        }
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
        var name :String = event.name;
        var index :int;
        if (name == "balls") {
            index = event.index;
            if (index != -1 && _balls[index] == null) {
                _balls[index] = new BallParticle(event.newValue[0], event.newValue[1],
                    Ball.RADIUS, index, false);
                _balls[index].wc = this;
                    
                APEngine.addParticle(_balls[index]);
                _ballLayer.addChild(_balls[index].ball);

                if (index == myIdx) {
                    _myBall = _balls[index];
                }

                // Zoom to it.
                panTo(event.newValue[0], event.newValue[1]);
            }

        } else if (name == "lastHit") {
            index = event.newValue[0];
            var x :Number = event.newValue[1];
            var y :Number = event.newValue[2];

            BallParticle(_balls[index]).addHitForce(x, y);

            if (gameCtrl.isMyTurn()) {
                _haveMoved = true;
            }

        } else if (name == "wickets") {
            index = event.index;
            var wicket :int = event.newValue as int;

            _wickets[index] = wicket;
            _status.targetWicket(index, wicket);

        } else {
            gameCtrl.localChat("unhandled prop change: " + name);
        }
    }

    /**
     * Notice that we passed a wicket.
     */
    public function passedWicket () :void
    {
        _moveAgain = true;
        _wickets[myIdx]++;
        gameCtrl.awardFlow(FLOW_MULTIPLIER * _wickets[myIdx]);
        if (_wickets[myIdx] >= map.wickets.length) {
            // That was the last one. Yay.
            gameCtrl.sendChat(gameCtrl.getOccupantName(gameCtrl.getMyId()) + " wins!");
            gameCtrl.endGame([gameCtrl.getMyId()]);

        } else {
            gameCtrl.set("wickets", _wickets[myIdx], myIdx);
        }
    }

    /**
     * Pans the view to be centered at the specified coordinate.
     */
    public function panTo (x :Number, y: Number) :void
    {
        // FIXME: Animate the pan to here, don't just snap
        /** Argh! I can't touch the stage to find out how big I am!
        _status.panTo(x - this.stage.stageWidth/2, y - this.stage.stageHeight/2);
        */
        _status.panTo(x - WIDTH/2, y - HEIGHT/2);
    }

    /** 
     * Sets things up and starts our own turn.
     */
    protected function startTurn () :void
    {
        _moveAgain = false;

        if(_myBall == null) {
            // It's the first time I've gone, so add my ball at the start

            gameCtrl.set("balls", [map.startPoint.x, map.startPoint.y], myIdx);

            // and target the first wicket
            gameCtrl.set("wickets", 0, myIdx);
        }

        _haveMoved = false;
    }

    protected var _currentPlayerIdx :int;

    protected var _lastMousePosition :Point = new Point(0, 0);

    protected var _haveMoved :Boolean;

    protected var _status :WonderlandStatus;

    protected var _spr :Sprite;

    protected var _ballLayer :Sprite;

    protected var _ballMarkerLayer :Sprite;

    protected var _board :WonderlandBoard;

    protected var _wickets :Array;

    protected var _balls :Array;

    protected var _myBall :BallParticle;

    protected var _moveAgain :Boolean;

    protected static const FLOW_MULTIPLIER :int = 50;

    protected static const AUTO_PAN_THRESHOLD :int = 200;

}
}
