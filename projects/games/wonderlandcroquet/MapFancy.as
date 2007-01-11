package {

import mx.core.MovieClipAsset;
import mx.core.SpriteAsset;
import org.cove.ape.Vector;
import flash.display.Sprite;
import flash.geom.Point;

public class MapFancy extends WonderlandMap
{
    public function MapFancy ()
    {
        super();

        startPoint.x = 900;
        startPoint.y = 1600;

        _rough = SpriteAsset(new Rough());
        _stone = SpriteAsset(new Stone());
        _background = MovieClipAsset(new Background());

        background.addChild(_rough);
        background.addChild(_stone);
        background.addChild(_background);

        addWalls(_background.width, _background.height);

        addObjects();

    }

    override protected function isRough (x :Number, y :Number) :Boolean
    {
        return hitTestSprite(x, y, _rough);
    }

    override protected function isStone (x :Number, y :Number) :Boolean
    {
        return hitTestSprite(x, y, _stone);
    }

    protected function hitTestSprite (x :Number, y :Number, spr :Sprite) :Boolean
    {
        var p :Point = spr.globalToLocal(new Point(x, y));
        return spr.hitTestPoint(x, y, true);
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
    protected static var Background :Class;
    [Embed (source="rsrc/course.swf#rough")]
    protected static var Rough :Class;
    [Embed (source="rsrc/course.swf#paving")]
    protected static var Stone :Class;
}
}
