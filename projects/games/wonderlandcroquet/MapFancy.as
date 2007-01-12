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

        _background = MovieClipAsset(new Background());

        background.addChildAt(_background, 0);
        // Temporarily rip out the terrain stuff; this gets broken depending
        // on what's on screen or not. I'll have to figure out how to really
        // do this...
        /*
        _rough = SpriteAsset(new Rough());
        _stone = SpriteAsset(new Stone());
        background.addChildAt(_rough, 0);
        background.addChildAt(_stone, 0);
        */

        addWalls(_background.width, _background.height);
    }

// Temporarily rip out the terrain stuff; this gets broken depending
// on what's on screen or not. I'll have to figure out how to really
// do this...
/*
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
*/
    // documentation inherited
    override protected function addObjects () :void
    {
        addPlanter(400, 730, 0, true, 11);
        addPlanter(400, 1070, 0, true, 337);
        addPlanter(1400, 730, 0, true, 108);
        addPlanter(1400, 1070, 0, true, 288);
        addPlanter(730, 400, 0, true, 78);
        addPlanter(1070, 400, 0, true, 322);
        addPlanter(730, 1400, 0, true, 343);
        addPlanter(1070, 1400, 0, true, 234);

/*
        addPlanter(850, 850, 45, true, 143);
        addPlanter(850, 950, 45, true, 324);
        addPlanter(950, 850, 45, true, 224);
        addPlanter(950, 950, 45, true, 229);
*/

        addPlanter(830, 900, 0, true, 32);
        addPlanter(900, 830, 0, true, 201);
        addPlanter(970, 900, 0, true, 303);
        addPlanter(900, 970, 0, true, 129);

        addBush(900, 900, 337);
    }

    // documentation inherited
    override protected function addWickets () :void
    {
        wickets = [
            new Wicket(1,  900,  1400, -90),
            new Wicket(2,  1200, 1200,  50),
            new Wicket(3,  1470, 1600, -145),
            new Wicket(4,  1550, 1000,   45),
            new Wicket(5,  1030,  700,  -75),
            new Wicket(6,  1100,  200,   90),
            new Wicket(7,   540,  400,   45),
            new Wicket(8,   200,  900,    0),
            new Wicket(9,   620,  800, -110),
            new Wicket(10,  555, 1400,   15),
        ];

        for each (var wicket :Wicket in wickets) {
           foreground.addChild(wicket);
        }
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
