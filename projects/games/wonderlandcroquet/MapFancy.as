package {

import mx.core.MovieClipAsset;
import mx.core.SpriteAsset;
import org.cove.ape.Vector;
import flash.display.Sprite;

public class MapFancy extends WonderlandMap
{
    public function MapFancy ()
    {
        super();

        _background = MovieClipAsset(new backgroundClass());
        background.addChild(_background);

        addWalls(_background.width, _background.height);

        addObjects();

        _rough = SpriteAsset(new roughClass());
        _stone = SpriteAsset(new stoneClass());
    }

    override protected function isRough (x :Number, y :Number) :Boolean
    {
        return _rough.hitTestPoint(x, y, true);
    }

    override protected function isStone (x :Number, y :Number) :Boolean
    {
        return _stone.hitTestPoint(x, y, true);
    }

    // Adds various boxes and whatnot around the scene
    protected function addObjects () :void
    {
        addPlanter(400, 730, 0, true, 11);
        addPlanter(400, 1070, 0, true, 337);
        addPlanter(1400, 730, 0, true, 108);
        addPlanter(1400, 1070, 0, true, 288);
        addPlanter(730, 400, 0, true, 78);
        addPlanter(1070, 400, 0, true, 322);
        addPlanter(730, 1400, 0, true, 343);
        addPlanter(1070, 1400, 0, true, 234);

        addPlanter(850, 850, 45, true, 143);
        addPlanter(850, 950, 45, true, 324);
        addPlanter(950, 850, 45, true, 224);
        addPlanter(950, 950, 45, true, 229);

        addPlanter(830, 900);
        addPlanter(900, 830);
        addPlanter(970, 900);
        addPlanter(900, 970);

        addBush(900, 900, 337);
    }

    // Our background movie
    protected var _background :MovieClipAsset;

    // The region defining our rough terrain
    protected var _rough :SpriteAsset;

    // The region defining our stone terrain
    protected var _stone :SpriteAsset;

    [Embed (source="rsrc/course.swf")]
    protected var backgroundClass :Class;
    [Embed (source="rsrc/course.swf#rough")]
    protected var roughClass :Class;
    [Embed (source="rsrc/course.swf#paving")]
    protected var stoneClass :Class;
}
}
