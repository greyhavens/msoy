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
        _startingPoints = new Array();
        _obstacles = new Array();
        _bonuses = new Array();
        var layerImg :DisplayObject = new objectsLayer() as DisplayObject;
        var layerBitmap :BitmapData = new BitmapData(layerImg.width, layerImg.height, true, 0);
        var trans :Matrix = new Matrix();
        trans.translate(layerImg.width / 2, layerImg.height / 2);
        layerBitmap.draw(layerImg, trans);
        var log :Log = Log.getLog(UnderwhirledDrift);
        for (var h :int = 0; h < layerImg.height; h++) {
            for (var w :int = 0; w < layerImg.width; w++) {
                if ((layerBitmap.getPixel32(w, h) & 0xFF000000) != 0) {
                    var obj :Object = map.get(layerBitmap.getPixel(w, h));
                    var point :Point = new Point(w - layerImg.width / 2, h - layerImg.height / 2);
                    switch (obj.type) {
                    case OBJECT_STARTING_LINE_POINT:
                        if (_startingLine == null) {
                            _startingLine = new Line(point, null);
                        } else {
                            _startingLine.stop = point;
                        }
                        break;
                    case OBJECT_STARTING_POSITION:
                        _startingPoints.push(point);
                        break;
                    case OBJECT_OBSTACLE:
                        _obstacles.push({cls: obj.cls, point: point});
                        break;
                    case OBJECT_BONUS:
                        _bonuses.push({cls: obj.cls, point: point});
                        break;
                    // ignore unknown colors
                    }
                }
            }
        }
        
        // sort the starting points based on distance from the starting line
        var angle :Number = Math.atan2(Math.abs(_startingLine.start.y - _startingLine.stop.y),
            Math.abs(_startingLine.start.x - _startingLine.stop.x));
        trans.identity();
        trans.translate(-_startingLine.start.x, -_startingLine.start.y);
        trans.rotate(-angle);
        var translatedY :Number = trans.transformPoint(_startingLine.stop).y;
        _startingPoints.sort(function (obj1 :Object, obj2 :Object) :int {
            var pnt1 :Point = obj1 as Point;
            var pnt2 :Point = obj2 as Point;
            var dist1 :Number = Math.abs(translatedY - trans.transformPoint(pnt1).y);
            var dist2 :Number = Math.abs(translatedY - trans.transformPoint(pnt2).y);
            return dist1 < dist2 ? -1 : (dist2 < dist1 ? 1 : 0);
        });
        log.debug(_startingPoints);
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
