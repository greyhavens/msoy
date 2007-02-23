package {

import flash.display.DisplayObject;
import flash.display.BitmapData;

import flash.geom.Point;
import flash.geom.Matrix;

import com.threerings.util.Line;
import com.threerings.util.HashMap;

public class LevelConfig 
{
    public static const OBJECT_STARTING_LINE_POINT :int = 1;
    public static const OBJECT_STARTING_POSITION   :int = 2;
    public static const OBJECT_OBSTACLE            :int = 3;
    public static const OBJECT_BONUS               :int = 4;

    public function LevelConfig (objectsLayer :Class, map :HashMap) 
    {
        var layerImg :DisplayObject = new objectsLayer() as DisplayObject;
        var layerBitmap :BitmapData = new BitmapData(layerImg.width, layerImg.height, true, 0);
        var trans :Matrix = new Matrix();
        trans.translate(layerImg.width / 2, layerImg.height / 2);
        layerBitmap.draw(layerImg, trans);
        var log :Log = Log.getLog(UnderwhirledDrift);
        for (var h :int = 0; h < layerImg.height; h++) {
            for (var w :int = 0; w < layerImg.width; w++) {
                if ((layerBitmap.getPixel32(w, h) & 0xFF000000) != 0) {
                    var logmsg :String = "found ";
                    var obj :Object = map.get(layerBitmap.getPixel(w, h));
                    switch (obj.type) {
                    case OBJECT_STARTING_LINE_POINT:
                        logmsg += "starting line point";
                        break;
                    case OBJECT_STARTING_POSITION:
                        logmsg += "starting position";
                        break;
                    case OBJECT_OBSTACLE:
                        logmsg += "obstacle";
                        break;
                    case OBJECT_BONUS:
                        logmsg += "bonus";
                        break;
                    // ignore unknown colors
                    }
                    logmsg += " @ (" + w + ", " + h + ")";
                    log.debug(logmsg);
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
