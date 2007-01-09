package {

import flash.display.Bitmap;
import flash.display.Graphics;
import flash.display.Sprite;

import flash.events.MouseEvent;
import flash.events.Event;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.MessageReceivedEvent;

import mx.core.*;
import mx.utils.ObjectUtil;

import org.cove.ape.*;

[SWF(width="750", height="508")]
public class WonderlandCroquet extends Sprite
    implements Game
{
    /** Size of our viewing area. */
    public static const WIDTH :int = 750;
    public static const HEIGHT :int = 508;


    public function WonderlandCroquet ()
    {
        var spr :Sprite = new Sprite();
        spr.addEventListener(MouseEvent.CLICK, mouseClicked);
        addEventListener(Event.ENTER_FRAME, run);

        addChild(spr);

        _drawArea = spr.graphics;

        //_map = new MapBasic();
        _map = new MapFancy();
        spr.addChild(_map.background);

        APEngine.init(1/3);
        APEngine.defaultContainer = this;

        for each (var particle :AbstractParticle in _map.particles) {
            APEngine.addParticle(particle);
        }

        // Add some balls
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
                Ball.RADIUS + (Math.random() * (WIDTH - (2 * Ball.RADIUS))), 
                Ball.RADIUS + (Math.random() * (HEIGHT - (2 * Ball.RADIUS))), 
                Ball.RADIUS, color, false);

            APEngine.addParticle(ball);
            spr.addChild(ball.ball);
        }

        _paintQueue = APEngine.getAll();

        spr.addChild(_map.foreground);

        _scroller = new WonderlandScroller(spr);
        addChild(_scroller);
        _scroller.x = 30;
        _scroller.y = 30;
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


    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        // set up our listeners
        _gameObj = gameObj;
        _gameObj.addEventListener(StateChangedEvent.GAME_STARTED, gameStarted);
        _gameObj.addEventListener(StateChangedEvent.GAME_ENDED, gameEnded);
        _gameObj.addEventListener(PropertyChangedEvent.TYPE, propChanged);

        // do some other fun stuff
        _gameObj.localChat("Wonderland Croquet");

        _drawArea.clear();

        // TODO: detect if the game is already in play
    }

    protected function gameStarted (event :StateChangedEvent) :void
    {
        _gameObj.localChat("GO!!!!");

        // start processing!
        mouseChildren = true;
    }

    protected function mouseClicked (event :MouseEvent) :void
    {
    }

    protected function propChanged (event :PropertyChangedEvent) :void
    {
    }

    protected function gameEnded (event :StateChangedEvent) :void
    {
        mouseChildren = false;
        var names :Array = _gameObj.getPlayerNames();
        for each (var idx :int in _gameObj.getWinnerIndexes()) {
            _gameObj.localChat(names[idx] + " has won!");
        }
    }

    protected var _map :WonderlandMap;

    protected var _scroller :WonderlandScroller;

    protected var _gameObj :EZGame;

    protected var _drawArea :Graphics;

    protected var _paintQueue :Array;
}
}
