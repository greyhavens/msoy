package {

import flash.display.Sprite;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;

[SWF(width="300", height="400")]
public class Breakthrough extends Sprite
    implements Game
{
    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        // create the game board
        addChild(new Board(gameObj));
    }
}
}
