package com.threerings.underwhirleddrift.scene {

import flash.display.Sprite;
import flash.display.Shape;
import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.geom.Point;
import flash.geom.Matrix;
import flash.geom.Rectangle;

import flash.utils.describeType;

import com.threerings.util.Line;

import com.threerings.underwhirleddrift.kart.KartObstacle;

public class Level extends Sprite
{
    public function Level (ground :Ground, background :Class, rough :Class, track :Class, 
        wall :Class, horizon :Class, flat :Class, config :LevelConfig) :void
    {
        _horizon = horizon;
        _ground = ground;
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

        _track = new track() as DisplayObject;
        _wall = new wall() as DisplayObject;
        addChild(new flat() as DisplayObject);

        _ground.setLevel(this);
        _ground.setScenery(_scenery = new Scenery(config.getObstacles().concat(
            config.getBonuses())));
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
        _ground.setKartLocation(_config.getStartingPoint(_startingPosition));
    }

    public function getFinishLine () :Line
    {
        return _config.getStartingLine();
    }
    
    public function getBoosts () :Array
    {
        return _config.getBoosts();
    }

    public function get horizon () :Class
    {
        return _horizon;
    }

    /**
     * Adds an opponent's kart to the fray. 
     */
    public function addOpponentKart (position :int, kartType :String) :KartObstacle
    {
        var kart :KartObstacle = new KartObstacle(_config.getStartingPoint(position), kartType, 
            _ground);
        _scenery.addKart(kart);
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
    protected var _horizon :Class;
}
}
