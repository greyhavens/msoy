package {
import flash.display.Sprite;
import flash.display.Shape;
import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.geom.Point;
import flash.geom.Matrix;
import flash.geom.Rectangle;

import mx.core.MovieClipAsset;

public class Track extends Sprite 
{
    public function Track ()
    {
        var backgroundImage :Shape;
        for (var ii :int = 0; ii < 6; ii++) {
            backgroundImage = new Shape();
            backgroundImage.graphics.beginBitmapFill((new BACKGROUND_IMAGE() as Bitmap).bitmapData);
            backgroundImage.graphics.drawRect(0, 0, Ground.IMAGE_SIZE * 2, Ground.IMAGE_SIZE * 2);
            backgroundImage.graphics.endFill();
            if (ii < 3) {
                backgroundImage.x = -Ground.IMAGE_SIZE * 2;
            } 
            if ((ii % 3) == 0) {
                backgroundImage.y = -Ground.IMAGE_SIZE * 3.5;
            } else if ((ii % 3) == 1) {
                backgroundImage.y = -Ground.IMAGE_SIZE * 1.5;
            } else {
                backgroundImage.y = Ground.IMAGE_SIZE * 0.5;
            }
            addChild(backgroundImage);
        }

        // this will eventually feature a seed distributed to each client
        _totalTracks = (new TRACKS() as MovieClipAsset).totalFrames;
        _trackIndices[3] = Math.ceil(Math.random() * _totalTracks);

        // generate all three tracks
        for (ii = 0; ii < 3; ii++) {
            moveTrackForward();
            addChild(_tracks[2-ii]);
        }
        // center all tracks on the beginning of the middle track - only necessary at startup
        var tempBitmap :BitmapData = new BitmapData(Ground.IMAGE_SIZE, Ground.IMAGE_SIZE, true, 0);
        var translate :Matrix = new Matrix();
        translate.translate(Ground.HALF_IMAGE_SIZE, Ground.HALF_IMAGE_SIZE);
        tempBitmap.draw(_tracks[1], translate);
        var pointLeft :Point = new Point(0, Ground.IMAGE_SIZE - 1);
        var pointRight :Point = new Point (0, Ground.IMAGE_SIZE - 1);
        for (; pointLeft.x < Ground.IMAGE_SIZE; pointLeft.x++, pointRight.x++) {
            if (tempBitmap.getPixel32(pointLeft.x, pointLeft.y) & 0xFF000000) break;
        }
        for (; pointRight.x < Ground.IMAGE_SIZE; pointRight.x++) {
            if ((tempBitmap.getPixel32(pointRight.x, pointRight.y) & 0xFF00000) == 0) break;
        }
        for (ii = 0; ii < 3; ii++) {
            _tracks[ii].x -= (pointRight.x + pointLeft.x) / 2 - Ground.HALF_IMAGE_SIZE;
        }
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
        for (var ii :int = 0; ii < 3; ii++) {
            _trackIndices[ii] = _trackIndices[ii+1];
            if (_trackIndices[ii] != -1) {
                _tracks[ii].gotoAndStop(_trackIndices[ii]);
                if (ii == 0) {
                    _tracks[ii].y = Ground.IMAGE_SIZE;
                } else if (ii == 2) {
                    _tracks[ii].y = -Ground.IMAGE_SIZE;
                } 
                if (ii == 2) {
                    if (_trackIndices[ii-1] != -1) alignTracks(_tracks[ii-1], _tracks[ii]);
                } else {
                    _tracks[ii].x = _tracks[ii+1].x;
                }
                if (ii == 1) {
                    var translate :Matrix = new Matrix();
                    translate.translate(Ground.HALF_IMAGE_SIZE, Ground.HALF_IMAGE_SIZE);
                }
            }
        }
        // next track
        _trackIndices[3] = Math.ceil(Math.random() * _totalTracks);

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
     * Returns true if the given point (in the local coordinates) is on the road surface.
     */
    public function isOnRoad (loc :Point) :Boolean
    {
        var track :DisplayObject;
        if (loc.y >= Ground.HALF_IMAGE_SIZE) {
            loc.y -= Ground.IMAGE_SIZE;
            track = _tracks[0];
        } else if (loc.y >= -Ground.HALF_IMAGE_SIZE) {
            track = _tracks[1];
        } else {
            loc.y += Ground.IMAGE_SIZE;
            track = _tracks[2];
        }
        loc.x -= track.x;
        var imgData :BitmapData = new BitmapData(1, 1, true, 0);
        var trans :Matrix = new Matrix();
        trans.translate(-loc.x, -loc.y);
        imgData.draw(track, trans);
        return (imgData.getPixel32(0, 0) & 0xFF000000) != 0;
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
        for (; bottomPoint.x < Ground.IMAGE_SIZE; bottomPoint.x++) {
            if (tempBitmap.getPixel32(bottomPoint.x, bottomPoint.y) & 0xFF000000) break;
        }
        tempBitmap = new BitmapData(Ground.IMAGE_SIZE, Ground.IMAGE_SIZE, true, 0);
        tempBitmap.draw(top, translate);
        var topPoint :Point = new Point(0, Ground.IMAGE_SIZE - 1);
        for (; topPoint.x < Ground.IMAGE_SIZE; topPoint.x++) {
            if (tempBitmap.getPixel32(topPoint.x, topPoint.y) & 0xFF000000) break;
        }
        // assume the bottom is or has been moved to 0
        top.x = bottomPoint.x - topPoint.x;
    }

    /*[Embed(source='rsrc/track.swf#track')]*/
    [Embed(source='rsrc/new_track.swf#track')]
    protected static const TRACKS :Class;

    [Embed(source='rsrc/blue_ground.png')]
    protected static const BACKGROUND_IMAGE :Class;

    /** Track data - keep in vector form for decent scaling */
    protected var _tracks :Array = [new TRACKS(), new TRACKS(), new TRACKS()];

    /** indices into the TRACKS array of available tracks */
    protected var _trackIndices :Array = [-1, -1, -1, -1];

    /** Number of tracks found in the track asset */
    protected var _totalTracks :int;
}
}
