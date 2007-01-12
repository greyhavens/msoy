package {
import flash.display.Sprite;
import flash.display.Shape;
import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.geom.Point;
import flash.geom.Matrix;

import mx.core.MovieClipAsset;

public class Track extends Sprite 
{
    public function Track ()
    {
        var backgroundImage :Shape;
        for (var i :int = 0; i < 4; i++) {
            backgroundImage = new Shape();
            backgroundImage.graphics.beginBitmapFill((new BACKGROUND_IMAGE() as Bitmap).bitmapData);
            backgroundImage.graphics.drawRect(0, 0, Ground.IMAGE_SIZE * 1.5, 
                Ground.IMAGE_SIZE * 1.5);
            backgroundImage.graphics.endFill();
            if (i > 1) {
                backgroundImage.x = -Ground.IMAGE_SIZE * 1.5;
            } 
            if (i == 1 || i == 3) {
                backgroundImage.y = -Ground.IMAGE_SIZE * 1.5;
            }
            addChild(backgroundImage);
        }

        // this will eventually feature a seed distributed to each client
        _totalTracks = (new TRACKS() as MovieClipAsset).totalFrames;
        Log.testing("total tracks: " + _totalTracks);
        _trackIndices[3] = Math.floor(Math.random() * _totalTracks);

        // generate all three tracks, and align them properly
        moveTrackForward();
        moveTrackForward();
        moveTrackForward();
    }

    /**
     * Cycles tracks from front to back, bringing in the next randomly generated track to the
     * front position.
     *
     * returns the amount to shift the camera in the x direction, in order to keep the tracks
     * centered in the display
     */
    public function moveTrackForward () :int
    {
        for (var i :int = 0; i < 3; i++) {
            if (_tracks[i] != null) {
                removeChild(_tracks[i]);
            }
            _trackIndices[i] = _trackIndices[i+1];
            if (_trackIndices[i] != -1) {
                _tracks[i] = new TRACKS();
                _tracks[i].gotoAndStop(_trackIndices[i]);
                if (i == 0) {
                    _tracks[i].y = Ground.IMAGE_SIZE;
                } else if (i == 2) {
                    _tracks[i].y = -Ground.IMAGE_SIZE;
                } 
                if (i == 2) {
                    if (_trackIndices[i-1] != -1) alignTracks(_tracks[i-1], _tracks[i]);
                } else {
                    _tracks[i].x = _tracks[i+1].x;
                }
                addChild(_tracks[i]);
            }
        }
        // next track
        _trackIndices[3] = Math.floor(Math.random() * _totalTracks);
        Log.testing("next track: " + _trackIndices[3]);

        var xShift :int = 0;
        if (_trackIndices[1] != -1) {
            xShift = -_tracks[1].x;
            _tracks[1].x += xShift;
            if (_trackIndices[0] != -1) {
                _tracks[0].x += xShift;
            }
        }
        return xShift;
    }

    /**
     * aligns the second track with a selected input of the first one.  Currently assumes that
     * the first track ends on the top edge and the second track begins on the bottom edge.
     */
    protected function alignTracks (bottom :DisplayObject, top :DisplayObject) :void
    {
        // the only way to get at pixel data directly is to draw the vector onto a bitmap.
        var tempBitmap :BitmapData = new BitmapData(Ground.IMAGE_SIZE, Ground.IMAGE_SIZE, true, 0);
        var translate :Matrix = new Matrix();
        translate.translate(Ground.HALF_IMAGE_SIZE, Ground.HALF_IMAGE_SIZE);
        tempBitmap.draw(bottom, translate);
        var bottomPoint :Point = new Point(0, 0);
        for (; bottomPoint.x < Ground.HALF_IMAGE_SIZE; bottomPoint.x++) {
            if (tempBitmap.getPixel32(bottomPoint.x, bottomPoint.y) & 0xFF000000) break;
        }
        tempBitmap = new BitmapData(Ground.IMAGE_SIZE, Ground.IMAGE_SIZE, true, 0);
        tempBitmap.draw(top, translate);
        var topPoint :Point = new Point(0, Ground.IMAGE_SIZE - 1);
        for (; topPoint.x < Ground.HALF_IMAGE_SIZE; topPoint.x++) {
            if (tempBitmap.getPixel32(topPoint.x, topPoint.y) & 0xFF000000) break;
        }
        // assume the bottom is or has been moved to 0
        top.x = bottomPoint.x - topPoint.x;
    }

    [Embed(source='rsrc/track.swf#track')]
    protected static const TRACKS :Class;

    [Embed(source='rsrc/blue_ground.png')]
    protected static const BACKGROUND_IMAGE :Class;

    /** Track data - keep in vector form for decent scaling */
    protected var _tracks :Array = new Array(3);

    /** indices into the TRACKS array of available tracks */
    protected var _trackIndices :Array = [-1, -1, -1, -1];

    /** Number of tracks found in the track asset */
    protected var _totalTracks :int;
}
}
