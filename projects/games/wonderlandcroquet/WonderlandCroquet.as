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
        addEventListener(Event.ENTER_FRAME, run);

        addChild(_spr);

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
        var colors :Array = [
            0x000000,
            0xff0000,
            0x00ff00,
            0x0000ff,
            0xffff00,
            0xff00ff,
            0x00ffff,
            0xffffff,
            0x440000,
            0x004400,
            0x000044,
            0x444400,
            0x440044,
            0x004444,
            0x444444,
        ];

        for each (var color :int in colors) {
            var ball: BallParticle = new BallParticle(
                Ball.RADIUS + (Math.random() * (800 - (2 * Ball.RADIUS))), 
                Ball.RADIUS + (Math.random() * (600 - (2 * Ball.RADIUS))), 
                Ball.RADIUS, color, false);

            APEngine.addParticle(ball);
            _spr.addChild(ball.ball);
        }
    }

    protected function run (evt :Event) :void
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
            _gameCtrl.localChat("Reversi superchallenge: go!");

            // configure the board
            //_board = new Board(_gameCtrl, BOARD_SIZE);

        } else if (event.type == StateChangedEvent.GAME_ENDED) {
            _gameCtrl.localChat("Thank you for playing Reversi!");

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

    /** Our game control object. */
    protected var _gameCtrl :EZGameControl;

}
}
