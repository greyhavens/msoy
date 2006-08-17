package {

import flash.display.Graphics;
import flash.display.Sprite;

import flash.events.MouseEvent;

import com.metasoy.game.Game;
import com.metasoy.game.GameObject;
import com.metasoy.game.PropertyChangedEvent;
import com.metasoy.game.StateChangedEvent;

[SWF(width="400", height="400")]
public class ClickFest extends Sprite
    implements Game
{
    public function ClickFest ()
    {
        var spr :Sprite = new Sprite();
        spr.addEventListener(MouseEvent.CLICK, mouseClicked);
        addChild(spr);

        _drawArea = spr.graphics;
    }

    // from Game
    public function setGameObject (gameObj :GameObject) :void
    {
        if (_gameObj != null) {
            return; // we already got one!
        }

        // set up our listeners
        _gameObj = gameObj;
        _gameObj.addEventListener(StateChangedEvent.GAME_STARTED, gameStarted);
        _gameObj.addEventListener(StateChangedEvent.GAME_ENDED, gameEnded);
        _gameObj.addEventListener(PropertyChangedEvent.TYPE, propChanged);

        // do some other fun stuff
        _gameObj.writeToLocalChat("Welcome to ClickFest!\n\n" +
            "The object of the game is simple: Click like the wind!\n\n" +
            "You are awarded " + POINTS_NEW + " point for clicking on a " +
            "new point, or " + POINTS_OVER_OTHER + " points for clicking on " +
            "a point of another player's color. Be careful, you'll get " +
            POINTS_OVER_SELF + " points for clicking on your own point.\n\n" +
            "The first player to " + SCORE_TO_WIN + " points wins.");

        _drawArea.clear();
        // must fill with black so that we get clicks
        _drawArea.beginFill(0x330000);
        _drawArea.drawRect(0, 0, 400, 400);

        // fill in any already-marked spots
        for (var key :String in _gameObj.data) {
            if (key.charAt(0) == "p") {
                drawClick(key, _gameObj.get(key));
            }
        }

        // TODO: detect if the game is already in play
    }

    protected function gameStarted (event :StateChangedEvent) :void
    {
        _myScore = 0;
        _gameObj.writeToLocalChat("GO!!!!");

        // start processing!
        mouseChildren = true;
    }

    protected function mouseClicked (event :MouseEvent) :void
    {
        var myIdx :int = _gameObj.getMyIndex();
        var key :String = "p" + event.localX + ":" + event.localY;

        var prev :Object = _gameObj.get(key);
        var points :int;
        if (prev == null) {
            points = POINTS_NEW;

        } else if (prev === myIdx) {
            points = POINTS_OVER_SELF;

        } else {
            points = POINTS_OVER_OTHER;
        }

        _gameObj.set(key, myIdx);

        _myScore += points;
        if (_myScore >= SCORE_TO_WIN) {
            _gameObj.endGame(myIdx);
        }
    }

    protected function propChanged (event :PropertyChangedEvent) :void
    {
        if (event.name.charAt(0) == "p") {
            drawClick(event.name, event.newValue);
        }
    }

    protected function drawClick (propName :String, value :Object) :void
    {
        var player :int = int(value);

        var coords :Array =
            propName.substr(1, propName.length - 1).split(":");
        var x :Number = parseInt(coords[0]);
        var y :Number = parseInt(coords[1]);

        _drawArea.beginFill(uint(COLORS[player]));
        _drawArea.drawRect(x, y, 1, 1);
    }

    protected function gameEnded (event :StateChangedEvent) :void
    {
        mouseChildren = false;
        var names :Array = _gameObj.getPlayerNames();
        for each (var idx :int in _gameObj.getWinnerIndexes()) {
            _gameObj.writeToLocalChat(names[idx] + " has won!");
        }
    }

    protected var _gameObj :GameObject;

    protected var _myScore :int;

    protected var _drawArea :Graphics;

    protected static const COLORS :Array = [ 0x66FF00 , 0x6600FF ];

    protected static const POINTS_NEW :int = 1;
    protected static const POINTS_OVER_OTHER :int = 5;
    protected static const POINTS_OVER_SELF :int = -10;
    protected static const SCORE_TO_WIN :int = 100;
}
}
