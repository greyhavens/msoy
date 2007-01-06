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
        var imageSize :int = 2880;
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
        
        var trackVector :IBitmapDrawable = new _trackImage();
        var transform :Matrix = new Matrix();
        /*transform.tx = transform.ty = imageSize / 2;
        transform.a = transform.d = imageSize / 1024;
        var fullTrackData :BitmapData = new BitmapData(imageSize, imageSize);
        fullTrackData.draw(new _trackImage(), transform);*/
        imageSize = 1024;
        var sliceData :BitmapData; 
        var sliceImg :Bitmap;
        var xScale :Number = 6;
        var thisHeight :Number = 20;
        var scaleFactor :Number = 0.007;
        var totalHeight :Number = 0;
        var stripHeight :Number = 2;
        var stripHeightCeiling :int;
        var xShift :Number = (imageSize - displayWidth) / 2;
        for (var strip :int = 0; totalHeight <= displayHeight / 2; strip++,
            stripHeight = (stripHeight - scaleFactor) > 1 ? stripHeight - scaleFactor : 1) {
            stripHeightCeiling = Math.round(stripHeight + 0.5);
            totalHeight += stripHeightCeiling;
            sliceData = new BitmapData(displayWidth, stripHeightCeiling);
            transform.identity();
            transform.scale((1 - totalHeight / (displayHeight / 2) * 0.9) * xScale, 
                1);
            transform.translate(imageSize / 2 - ((imageSize - displayWidth) / 4), 
                imageSize / 2 - (imageSize - totalHeight + stripHeight));
            sliceData.draw(trackVector, transform);
            sliceImg = new Bitmap(sliceData);
            sliceImg.y = displayHeight - totalHeight + thisHeight;
            addChild(sliceImg);
        }
        /*for (var strip :int = 0; totalHeight <= displayHeight/2; strip++,
            stripHeight = (stripHeight - scaleFactor) > 1 ? stripHeight - scaleFactor : 1) {
            totalHeight += stripHeight;
            Log.testing("strip: " + stripHeight + ", total: " + totalHeight);
            sliceData = new BitmapData(imageSize, thisHeight);
            sliceData.copyPixels(fullTrackData,
                new Rectangle(0, imageSize - strip * thisHeight, imageSize, thisHeight),  
                new Point(0, 0));
            sliceImg = new Bitmap(sliceData);
            sliceImg.y = displayHeight - totalHeight + thisHeight;
            sliceImg.height = Math.round(stripHeight+0.5);
            Log.testing("y: " + sliceImg.y + ", height: " + sliceImg.height);
            sliceImg.scaleX = (1 - (totalHeight / (displayHeight / 2)) * 0.9) * xScale;
            sliceImg.x = (imageSize - sliceImg.width) / 2 - xShift;
            addChild(sliceImg);
        }*/
    }

    /** track image */
    [Embed(source='rsrc/test_track.swf#test')]
    protected var _trackImage :Class;
}
}
