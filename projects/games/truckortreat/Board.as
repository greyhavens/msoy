package {

import flash.display.Bitmap;

public class Board extends BaseSprite
{   
    /** Lists of sidewalk coordinates. These are safe spots to start on. */
    public static const SIDEWALK_X :Array = [];
    public static const SIDEWALK_Y :Array = [];
    
    /** The y coordinate of the horizon line. */
    public static const HORIZON :int = 157;
    
    public function Board ()
    {
        super(0, 0, Bitmap(new backgroundAsset()));
    }
    
    /** Background image. */
    [Embed(source="rsrc/background.png")]
    protected var backgroundAsset :Class;
}
}
