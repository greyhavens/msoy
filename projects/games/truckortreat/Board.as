package {

import flash.display.Sprite;
import flash.display.Bitmap;
import flash.display.BitmapData;
import com.threerings.ezgame.EZGame;

public class Board extends Sprite
{   
    /** Lists of sidewalk coordinates. These are safe spots to start on. */
    public static const SIDEWALK_X :Array = [];
    public static const SIDEWALK_Y :Array = [];
    
    public function Board (gameObj :EZGame)
    {
        var bitmap :Bitmap = Bitmap(new backgroundAsset());
        _width = bitmap.bitmapData.width;
        _height = bitmap.bitmapData.height;
        addChild(bitmap);
    }
    
    public function getWidth() :int 
    {
        return _width;
    }
    
    public function getHeight() :int
    {
        return _height;
    }
    
    protected function detectCollions() :void
    {
        
    }
    
    /** Dimensions of board. */
    protected var _width :int;
    protected var _height :int;
    
    protected var _kids :Array = [];
    
    protected var _cars :Array = [];
    
    /** Background image. */
    [Embed(source="rsrc/background.png")]
    protected var backgroundAsset :Class;
}
}
