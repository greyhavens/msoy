package {

import flash.display.DisplayObject;
import flash.display.BitmapData;

import flash.geom.Point;

import com.threerings.util.Line;

public class LevelConfig 
{
    public function LevelConfig (objectsLayer :Class, mapping :Object) 
    {
        var layerImg :DisplayObject = new objectsLayer() as DisplayObject;
        var layerBitmap :BitmapData = new BitmapData(layerImg.width, layerImg.height, true, 0);
        layerBitmap.draw(layerImg);
        var log :Log = Log.getLog(UnderwhirledDrift);
        for (var h :int = 0; h < layerImg.height; h++) {
            for (var w :int = 0; w < layerImg.width; w++) {
                if ((layerBitmap.getPixel32(w, h) & 0xFF000000) != 0) {
                    log.debug("found color: " + layerBitmap.getPixel(w, h).toString(16) + " @ (" + 
                        w + ", " + h + ")");
                }
            }
        }
    }

    public function getStartingLine () :Line
    {
        return _startingLine;
    }

    public function getStartingPoint (position :int) :Point
    {
        return _startingPoints[position];
    }

    public function getObstacles () :Array
    {
        return _obstacles;
    }

    public function getBonuses () :Array 
    {
        return _bonuses;
    }

    protected var _startingLine :Line;
    protected var _startingPoints :Array;
    protected var _obstacles :Array;
    protected var _bonuses :Array;
}
}
