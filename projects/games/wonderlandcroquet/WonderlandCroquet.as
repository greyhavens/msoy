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
        addEventListener(Event.ENTER_FRAME, tick);

        addChild(_spr);

        // TODO: support better map loading, choice at table time,
        // or maybe just pick one randomly
        //_map = new MapBasic();
        _map = new MapFancy();
        _spr.addChild(_map.background);

        APEngine.init(1/3);
        APEngine.defaultContainer = this;

        for each (var particle :AbstractParticle in _map.particles) {
            APEngine.addParticle(particle);
        }

        // Add some balls
        addRandomBalls();

        _paintQueue = APEngine.getAll();

        _spr.addChild(_map.foreground);

        _scroller = new WonderlandScroller(_spr);
        addChild(_scroller);
        _scroller.x = 30;
        _scroller.y = 30;
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
            _spr.addChild(ball.ball);
        }
    }

    protected function tick (evt :Event) :void
    {
        var ii :int;
       
        for (ii = 0; ii < _paintQueue.length; ii++) {
            if (_paintQueue[ii] is BallParticle) {
                _map.applyModifierForce(BallParticle(_paintQueue[ii]));
            }
        }

        APEngine.step();

        for (ii = 0; ii < _paintQueue.length; ii++) {
            _paintQueue[ii].paint();
        }
    }

    // from StateChangedListener
    public function stateChanged (event :StateChangedEvent) :void
    {
        if (event.type == StateChangedEvent.TURN_CHANGED) {
        /*
            if (_pieces == null) {
                // if we're the first player, we take care of setting up the
                // board
                if (_gameCtrl.isMyTurn()) {
                    //_board.initialize();
                    _gameCtrl.set("startGame", true);
                    //setUpPieces();
                }

            } else {
                //showMoves();
            }

*/
        } else if (event.type == StateChangedEvent.GAME_STARTED) {
            _gameCtrl.localChat("Wonderland Croquet!");

            // configure the board
            //_board = new Board(_gameCtrl, BOARD_SIZE);

        } else if (event.type == StateChangedEvent.GAME_ENDED) {
            _gameCtrl.localChat("Off with your head!");

        }
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
        var name :String = event.name;
        if (name == "board") {
            if (event.index != -1) {
                // read the change
                //readBoard();

            //} else if (_pieces == null) {
                // the other player has initialized the game
                //setUpPieces();
            }
        }
    }

    protected var _map :WonderlandMap;

    protected var _scroller :WonderlandScroller;

    protected var _spr :Sprite;

    protected var _paintQueue :Array;

    protected var _board :WonderlandBoard;
    protected var _wickets :Array;

    /** Our game control object. */
    protected var _gameCtrl :EZGameControl;

}
}
