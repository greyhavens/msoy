package {

import flash.display.Sprite;
import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.Shape;

import flash.geom.Rectangle;
import flash.geom.Point;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;

[SWF(width="400", height="400")]
public class UnderworldDrift extends Sprite
    implements Game
{
    public function UnderworldDrift ()
    {
        var imageSize :int = 1024;
        // inspired by 16:9 widescreen ratio
        var displayWidth :int = 711; 
        var displayHeight :int = 400;
        var masker :Shape = new Shape();
        masker.graphics.beginFill(0xFFFFFF);
        masker.graphics.drawRect(0, 0, displayWidth, displayHeight);
        masker.graphics.endFill();
        this.mask = masker;
        addChild(masker);

        var colorBackground :Shape = new Shape();
        colorBackground.graphics.beginFill(0x8888FF);
        colorBackground.graphics.drawRect(0, 0, displayWidth, displayHeight);
        colorBackground.graphics.endFill();
        addChild(colorBackground); 

        var fullTrack :Bitmap = new _trackImage();
        var fullTrackData :BitmapData = fullTrack.bitmapData;
        var sliceData :BitmapData; 
        var sliceImg :Bitmap;
        var xScale :Number = 10;
        var thisHeight :Number = 3.5;
        var scaleFactor :Number = 0.3;
        var totalHeight :Number = 0;
        var stripHeight :Number = 4;
        var xShift :Number = (imageSize - displayWidth) / 2;
        for (var strip :int = 0; totalHeight <= displayHeight/2; strip++,
            stripHeight = (stripHeight - scaleFactor) > 1 ? stripHeight - scaleFactor : 1) {
            totalHeight += stripHeight;
            sliceData = new BitmapData(imageSize, thisHeight);
            sliceData.copyPixels(fullTrackData,
                new Rectangle(0, imageSize - strip * thisHeight, imageSize, thisHeight),  
                new Point(0, 0));
            sliceImg = new Bitmap(sliceData);
            sliceImg.y = displayHeight - totalHeight + thisHeight;
            sliceImg.height = stripHeight;
            sliceImg.scaleX = (1 - (totalHeight / (displayHeight / 2)) * 0.9) * xScale;
            sliceImg.x = (imageSize - sliceImg.width) / 2 - xShift;
            addChild(sliceImg);
        }
    }

    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        _gameObject = gameObj;
    }

    /** Our game object. */
    protected var _gameObject :EZGame;

    /** track image */
    [Embed(source='rsrc/test_track.png')]
    protected var _trackImage :Class;
}
}
