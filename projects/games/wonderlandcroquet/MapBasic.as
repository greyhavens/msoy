package {

import mx.core.MovieClipAsset;
import mx.core.SpriteAsset;
import org.cove.ape.Vector;
import flash.display.Sprite;

public class MapBasic extends WonderlandMap
{
    public function MapBasic ()
    {
        background.addChild(new Background());

        addObjects();

        _rough = SpriteAsset(new Rough());
        _stone = SpriteAsset(new Stone());
    }

    // Adds various boxes and whatnot around the scene
    protected function addObjects () :void
    {
        addPlanter(589.3, 101.1, 15);
        addPlanter(560.8, 207.3, 15);
        addPlanter(379.6, 453.9, -10.5);
        addPlanter(477.9, 435.6, 79.5);
        addPlanter(576.1, 417.4, -10.5);
        addPlanter(674.4, 399.1, -10.5);

        addBush(553.7, 64.8, 15);
        addBush(534.0, 162.7, 128);
        addBush(429.4, 404.6, 12.5);
    }

    // The region defining our rough terrain
    protected var _rough :SpriteAsset;

    // The region defining our stone terrain
    protected var _stone :SpriteAsset;

    [Embed (source="rsrc/terrain_06.swf")]
    protected static var Background :Class;
    [Embed (source="rsrc/terrain_06.swf#rough")]
    protected static var Rough :Class;
    [Embed (source="rsrc/terrain_06.swf#paving")]
    protected static var Stone :Class;
}
}
