package {

import flash.display.Sprite;
import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.IBitmapDrawable;
import flash.display.Shape;

import flash.geom.Rectangle;
import flash.geom.Point;
import flash.geom.Matrix;

import com.threerings.ezgame.EZGameControl;

[SWF(width="400", height="400")]
public class UnderworldDrift extends Sprite
{
    public function UnderworldDrift ()
    {
        var imageSize :int = 1024;
        var halfImageSize :int = imageSize / 2;
        // inspired by 16:9 widescreen ratio
        var displayWidth :int = 711; 
        var displayHeight :int = 400;
        var masker :Shape = new Shape();
        masker.graphics.beginFill(0xFFFFFF);
        masker.graphics.drawRect(0, 0, displayWidth, displayHeight);
        masker.graphics.endFill();
        this.mask = masker;
        addChild(masker);

        // "sky"
        var colorBackground :Shape = new Shape();
        colorBackground.graphics.beginFill(0x8888FF);
        colorBackground.graphics.drawRect(0, 0, displayWidth, displayHeight / 2);
        colorBackground.graphics.endFill();
        addChild(colorBackground);

        // "ground"
        var background :Shape = new Shape();
        background.graphics.beginBitmapFill((new _backgroundImage() as Bitmap).bitmapData);
        // draw over the same coordinates as road tiles, so that the same transform applies
        background.graphics.drawRect(-halfImageSize, -halfImageSize, imageSize, imageSize);
        background.graphics.endFill();
        
        var trackVector :IBitmapDrawable = new _trackImage();
        var transform :Matrix = new Matrix();
        var sliceData :BitmapData; 
        var sliceImg :Bitmap;
        var xScale :Number = 7;
        var thisHeight :Number = 9;
        var scaleFactor :Number = 0.05;
        var totalHeight :Number = 0;
        var stripHeight :Number = 4;
        var stripHeightCeiling :int;
        var stripWidth :Number = 0;
        // align the vector so that the the display area is centered
        var xShift :Number = halfImageSize - (imageSize - displayWidth) / 2;
        // align the bottom of the vector with the top of the display area.
        var yShift :Number = 0 - halfImageSize;
        for (var strip :int = 0; totalHeight <= displayHeight / 2; strip++,
            stripHeight = (stripHeight - scaleFactor) > 1 ? stripHeight - scaleFactor : 1) {
            stripHeightCeiling = Math.round(stripHeight + 0.49);
            totalHeight += stripHeightCeiling;
            sliceData = new BitmapData(displayWidth, stripHeightCeiling, true, 0x000000);
            // TODO: figure out why this only works if we scale-x, translate both, then scale-y
            transform.identity();
            transform.scale(xScale * (1 - totalHeight / (displayHeight / 2) * 0.9), 1);
            transform.translate(xShift, yShift + strip * thisHeight + thisHeight);
            transform.scale(1, stripHeightCeiling / thisHeight);
            sliceData.draw(background, transform);
            sliceData.draw(trackVector, transform);
            sliceImg = new Bitmap(sliceData);
            sliceImg.y = displayHeight - totalHeight;
            addChild(sliceImg);
        }
    }

    /** track image */
    //[Embed(source='rsrc/test_track.swf#test')]
    [Embed(source='rsrc/track.swf#track3')]
    protected var _trackImage :Class;

    /** test background tile image */
    [Embed(source='rsrc/pixel_magma.png')]
    protected var _backgroundImage :Class;
}
}
