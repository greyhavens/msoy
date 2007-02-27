package {

import flash.display.Bitmap;
import flash.display.Graphics;
import flash.display.Sprite;

import flash.events.MouseEvent;
import flash.events.Event;

import com.threerings.ezgame.EZGameControl;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;

import mx.core.*;
import mx.utils.ObjectUtil;

import org.cove.ape.*;

[SWF(width="750", height="508")]
public class WonderlandCroquet extends Sprite
    implements PropertyChangedListener, StateChangedListener
{
    public function WonderlandCroquet ()
    {
        _gameCtrl = new EZGameControl(this);
        _gameCtrl.registerListener(this);

        _spr = new Sprite();
        _ballLayer = new Sprite();
        addEventListener(Event.ENTER_FRAME, tick);

        addChild(_spr);

        // TODO: support better map loading, choice at table time,
        // or maybe just pick one randomly
        //_map = new MapBasic();
        _map = new MapFancy();
        _spr.addChild(_map.background);
        _map.background.addEventListener(MouseEvent.MOUSE_DOWN, mouseDown);
        _map.background.addEventListener(MouseEvent.MOUSE_UP, mouseUp);

        _spr.addChild(_ballLayer);

        APEngine.init(1/3);
        APEngine.defaultContainer = this;

        for each (var particle :AbstractParticle in _map.particles) {
            APEngine.addParticle(particle);
        }

        _spr.addChild(_map.foreground);

        _scroller = new WonderlandScroller(_spr);
        addChild(_scroller);
        _scroller.x = _scroller.width / 2 + 5;
        _scroller.y = _scroller.height / 2 + 5;
    }

    protected function mouseDown (event :MouseEvent) :void
    {
       _spr.startDrag();
    }

    protected function mouseUp (event :MouseEvent) :void
    {
       _spr.stopDrag();
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
                _map.startPoint.x + (Math.cos(angle) * r),
                _map.startPoint.y + (Math.sin(angle) * r),
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
                _map.applyModifierForce(BallParticle(particle));
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

        if (_haveMoved && doneMoving && _gameCtrl.isMyTurn()) {
            _gameCtrl.endTurn();
        }
    }

    // from StateChangedListener
    public function stateChanged (event :StateChangedEvent) :void
    {
        if (event.type == StateChangedEvent.TURN_CHANGED) {
            if (_gameCtrl.isMyTurn()) {
                _gameCtrl.localChat("My turn!");
                var coord :Array = [];

                if(_myBall == null) {
                    // It's the first time I've gone, so add my ball at the start

                    _gameCtrl.localChat("Adding my ball: " + _myIdx);
                    coord = [_map.startPoint.x, _map.startPoint.y]; 
                    _gameCtrl.set("balls", coord, _myIdx);
                } else {
                    coord = [_balls[_myIdx].px, _balls[_myIdx].py];
                }

                _haveMoved = false;
                panTo(coord[0], coord[1]);
            }
        } else if (event.type == StateChangedEvent.GAME_STARTED) {
            _gameCtrl.localChat("Wonderland Croquet!");

            _balls = [];
            _myIdx = _gameCtrl.seating.getMyPosition();
            if (_myIdx == 0) {
                // FIXME: I'm not quite happy with this, but if I just set it, it doesn't appear
                // to have taken effect by the time it's my turn and I need to actually add a
                // ball
                _gameCtrl.setImmediate("balls", _balls);
            }

        } else if (event.type == StateChangedEvent.GAME_ENDED) {
            _gameCtrl.localChat("Off with your head!");

        }
    }

    /**
     * Pans the view to the specified coordinate.
     */
    protected function panTo (x :Number, y: Number) :void
    {
        _gameCtrl.localChat("Pan to " + x + ", " + y);

        // FIXME: Animate the pan to here, don't just snap
        _spr.x = - (x - this.stage.stageWidth/2);
        _spr.y = - (y - this.stage.stageHeight/2);
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
        var name :String = event.name;
        var index :int;
        _gameCtrl.localChat("prop change: " + name);
        if (name == "balls") {
            index = event.index;
            _gameCtrl.localChat("ball change: " + index);
            if (index != -1 && _balls[index] == null) {

                _gameCtrl.localChat("Inserting ball at " + index);
                _balls[index] = new BallParticle(event.newValue[0], event.newValue[1],
                    Ball.RADIUS, index, false);
                    
                APEngine.addParticle(_balls[index]);
                _ballLayer.addChild(_balls[index].ball);

                if (index == _myIdx) {
                    _myBall = _balls[index];
                    _myBall.gameCtrl = _gameCtrl;
                }

            }
        } else if (name == "lastHit") {
            index = event.newValue[0];
            var x :Number = event.newValue[1];
            var y :Number = event.newValue[2];

            BallParticle(_balls[index]).addHitForce(x, y);

            if (_gameCtrl.isMyTurn()) {
                _haveMoved = true;
            }
        }
    }

    protected var _haveMoved :Boolean;

    protected var _map :WonderlandMap;

    protected var _scroller :WonderlandScroller;

    protected var _spr :Sprite;

    protected var _ballLayer :Sprite;

    protected var _board :WonderlandBoard;

    protected var _wickets :Array;

    protected var _balls :Array;

    protected var _myBall :BallParticle;

    protected var _myIdx :int;

    /** Our game control object. */
    protected var _gameCtrl :EZGameControl;
}
}
