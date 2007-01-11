package {
import flash.display.Sprite;
import flash.display.Shape;
import flash.display.Bitmap;
import flash.display.DisplayObject;

import mx.core.MovieClipLoaderAsset;

import com.threerings.util.Random;

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
        _trackGenerator = new Random();
        _trackIndices[3] = _trackGenerator.nextInt(TRACKS.length);

        // generate all three tracks, and align them properly
        moveTrackForward();
        moveTrackForward();
        moveTrackForward();
    }

    /**
     * Rotates tracks from front to back, bringing in the next randomly generated track to the
     * front position.
     */
    public function moveTrackForward () :void
    {
        for (var i :int = 0; i < 3; i++) {
            if (_tracks[i] != null) {
                removeChild(_tracks[i]);
            }
            _trackIndices[i] = _trackIndices[i+1];
            if (_trackIndices[i] != -1) {
                _tracks[i] = new TRACKS[_trackIndices[i]];
                if (i == 0) {
                    _tracks[i].y = Ground.IMAGE_SIZE;
                } else if (i == 2) {
                    _tracks[i].y = -Ground.IMAGE_SIZE;
                }
                addChild(_tracks[i]);
            }
        }
        // next track
        _trackIndices[3] = _trackGenerator.nextInt(TRACKS.length);
    }

    [Embed(source='rsrc/track.swf#track1')]
    protected static const TRACK_1 :Class;

    [Embed(source='rsrc/track.swf#track2')]
    protected static const TRACK_2 :Class;

    [Embed(source='rsrc/track.swf#track3')]
    protected static const TRACK_3 :Class;

    protected static const TRACKS :Array = [TRACK_1, TRACK_2, TRACK_3 ];

    [Embed(source='rsrc/blue_ground.png')]
    protected static const BACKGROUND_IMAGE :Class;

    /** Track data */
    protected var _tracks :Array = new Array(3);
    protected var _trackIndices :Array = [-1, -1, -1, -1];

    /** Seedable random number generator.  The seed will be distributed to all clients. */
    protected var _trackGenerator :Random;
}
}
