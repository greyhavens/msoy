package {

import flash.display.Bitmap;

public class Board extends BaseSprite
{   
    /** Lists of sidewalk coordinates. These are safe spots to start on. */
    public static const SIDEWALK_X :Array = [];
    public static const SIDEWALK_Y :Array = [];
    
    public function Board ()
    {
        super(0, 0, Bitmap(new backgroundAsset()));
    }
    
    protected function detectCollions() :void
    {
        
    }
    
    protected var _kids :Array = [];
    
    protected var _cars :Array = [];
    
    /** Background image. */
    [Embed(source="rsrc/background.png")]
    protected var backgroundAsset :Class;
}
}
