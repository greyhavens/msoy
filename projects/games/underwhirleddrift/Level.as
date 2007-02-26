package {

import flash.display.Sprite;
import flash.display.Shape;
import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.geom.Point;
import flash.geom.Matrix;
import flash.geom.Rectangle;

public class Level extends Sprite
{
    public function Level (ground :Ground)
    {
        _ground = ground;
    }

    public function initialize (background :Class, rough :Class, track :Class, wall :Class,
        config :LevelConfig, /** this is temporary! */ objects :Class) :void
    {
        _config = config;
        var backgroundImage :Shape;
        var backgroundSprite :Sprite = (new background() as Sprite);
        var backgroundBitmap :BitmapData = new BitmapData(backgroundSprite.width, 
            backgroundSprite.height);
        var backgroundTrans :Matrix = new Matrix();
        backgroundTrans.translate(backgroundSprite.width / 2, backgroundSprite.height / 2);
        backgroundBitmap.draw(backgroundSprite, backgroundTrans);
        for (var ii :int = 0; ii < 4; ii++) {
            backgroundImage = new Shape();
            backgroundImage.graphics.beginBitmapFill(backgroundBitmap);
            // TODO: have background tiling react to the size of the level map properly
            backgroundImage.graphics.drawRect(0, 0, 1536, 1536);
            backgroundImage.graphics.endFill();
            if (ii < 2) {
                backgroundImage.x = -1536;
            }
            if ((ii % 2) == 0) {
                backgroundImage.y = -1536;
            }
            addChild(backgroundImage);
        }

        addChild(new rough() as DisplayObject);
        addChild(_track = (new track() as DisplayObject));
        addChild(_wall = (new wall() as DisplayObject));
        addChild(new objects() as DisplayObject);

        // because we're waiting both to hear from the host and from the loader, it is not known
        // whether the starting position or the config will be inialized first
        if (_startingPosition != -1) {
            _ground.setKartLocation(_config.getStartingPoint(_startingPosition));
        }
        _ground.setScenery(_scenery = new Scenery(config.getObstacles().concat(
            config.getBonuses())));
        for (ii = 0; ii < _kartsToAdd.length; ii++) {
            _scenery.addKart(_kartsToAdd[ii] as KartObstacle);
        }
    }

    public function isOnRoad (loc :Point) :Boolean
    {
        return isNotTransparent(loc, _track);
    }

    public function isOnWall (loc :Point) :Boolean
    {
        return isNotTransparent(loc, _wall);
    }

    public function setStartingPosition (position :int) :void
    {
        _startingPosition = position;
        if (_config != null) {
            _ground.setKartLocation(_config.getStartingPoint(_startingPosition));
        }
    }

    /**
     * Adds an opponent's kart to the fray. 
     */
    public function addOpponentKart (position :int) :KartObstacle
    {
        var kart :KartObstacle = new KartObstacle(_config.getStartingPoint(position));
        if (_scenery != null) {
            _scenery.addKart(kart);
        } else {
            _kartsToAdd.push(kart);
        }
        return kart;
    }

    protected function isNotTransparent (loc :Point, img :DisplayObject) :Boolean 
    {
        if (img == null) {
            return false;
        }
        var imgData :BitmapData = new BitmapData(1, 1, true, 0);
        var trans :Matrix = new Matrix();
        trans.translate(-loc.x, -loc.y);
        imgData.draw(img, trans);
        return (imgData.getPixel32(0, 0) & 0xFF000000) != 0;
    }

    protected var _track :DisplayObject;
    protected var _wall :DisplayObject;
    protected var _ground :Ground;
    protected var _config :LevelConfig;
    protected var _startingPosition :int = -1;
    protected var _scenery :Scenery;

    /** Opponent karts to add, once the scenery is available */
    protected var _kartsToAdd :Array = new Array();
}
}
