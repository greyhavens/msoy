package {
import flash.display.Sprite;
import flash.display.Shape;
import flash.display.Bitmap;

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

        addChild(new TRACK_2());
        var track3 :Sprite = new TRACK_3();
        track3.y = -1024;
        addChild(track3);
        var track1 :Sprite = new TRACK_1();
        track1.y = 1024;
        addChild(track1);
    }

    [Embed(source='rsrc/track.swf#track1')]
    protected static const TRACK_1 :Class;

    [Embed(source='rsrc/track.swf#track2')]
    protected static const TRACK_2 :Class;

    [Embed(source='rsrc/track.swf#track3')]
    protected static const TRACK_3 :Class;

    [Embed(source='rsrc/blue_ground.png')]
    protected static const BACKGROUND_IMAGE :Class;
}
}
