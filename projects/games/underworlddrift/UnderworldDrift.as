package {

import flash.display.Sprite;
import flash.display.Bitmap;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;

[SWF(width="400", height="400")]
public class UnderworldDrift extends Sprite
    implements Game
{
    public function UnderworldDrift ()
    {
        var trackImage :Bitmap = new _trackImage();
        addChild(trackImage);
    }

    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        _gameObject = gameObj;
    }

    /** Our game object. */
    protected var _gameObject :EZGame;

    /** track image */
    [Embed(source='rsrc/test_track.png')]
    protected var _trackImage :Class;
}
}
