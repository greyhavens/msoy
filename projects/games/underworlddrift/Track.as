package {
import flash.display.Sprite;
import flash.display.Shape;
import flash.display.Bitmap;
import flash.display.DisplayObject;

import mx.core.MovieClipLoaderAsset;

public class Track extends Sprite 
{
    public function Track ()
    {
        var backgroundImage :Shape;
        for (var i :int = 0; i < 3; i++) {
            backgroundImage = new Shape();
            backgroundImage.graphics.beginBitmapFill((new BACKGROUND_IMAGE() as Bitmap).bitmapData);
            backgroundImage.graphics.drawRect(-Ground.IMAGE_SIZE, -Ground.IMAGE_SIZE,
                Ground.IMAGE_SIZE * 2, Ground.IMAGE_SIZE * 2);
            backgroundImage.graphics.endFill();
            addChild(backgroundImage);

            if (i == 1) {
                backgroundImage.y = -Ground.IMAGE_SIZE * 2;
            } else if (i == 2) {
                backgroundImage.y = Ground.IMAGE_SIZE * 2;
            }
        }

        setBehindTrack(0);
        setCurrentTrack(1);
        setFrontTrack(2);
    }

    public function moveTrackForward () :void
    {
        setBehindTrack(_trackCurrentIndex);
        setCurrentTrack(_trackFrontIndex);
        setFrontTrack(_currentIndex);
        _currentIndex = (_currentIndex + 1) % 3;
    }

    protected function setBehindTrack (newTrack :int) :void
    {
        if (_trackBehind != null) {
            removeChild(_trackBehind);
        }
        _trackBehindIndex = newTrack;
        _trackBehind = new TRACKS[newTrack]();
        _trackBehind.y = Ground.IMAGE_SIZE;
        addChild(_trackBehind);
    }

    protected function setFrontTrack (newTrack :int) :void
    {
        if (_trackFront != null) {
            removeChild(_trackFront);
        }
        _trackFrontIndex = newTrack;
        _trackFront = new TRACKS[newTrack]();
        _trackFront.y = -Ground.IMAGE_SIZE;
        addChild(_trackFront);
    }

    protected function setCurrentTrack (newTrack :int) :void
    {
        if (_trackCurrent != null) {
            removeChild(_trackCurrent);
        }
        _trackCurrentIndex = newTrack;
        _trackCurrent = new TRACKS[newTrack]();
        addChild(_trackCurrent);
    }

    [Embed(source='rsrc/track.swf#track1')]
    protected static const TRACK_1 :Class;

    [Embed(source='rsrc/track.swf#track2')]
    protected static const TRACK_2 :Class;

    [Embed(source='rsrc/track.swf#track3')]
    protected static const TRACK_3 :Class;

    protected static const TRACKS :Array = [
        TRACK_1, TRACK_2, TRACK_3 ];

    [Embed(source='rsrc/blue_ground.png')]
    protected static const BACKGROUND_IMAGE :Class;

    /** Tracks surrounding the current position */
    protected var _trackBehind :DisplayObject;
    protected var _trackCurrent :DisplayObject;
    protected var _trackFront :DisplayObject;

    /** Index in the tracks array for the current tracks */
    protected var _trackBehindIndex :int;
    protected var _trackCurrentIndex :int;
    protected var _trackFrontIndex :int;

    /** temporary!  Will be replaced by seedable random number generator */
    protected var _currentIndex :int = 0;
}
}
