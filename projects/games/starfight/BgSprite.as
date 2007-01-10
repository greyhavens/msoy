package {

import flash.display.Sprite;
import flash.display.Shape;
import flash.display.Bitmap;

public class BgSprite extends Sprite
{
    public var boardWidth :int;
    public var boardHeight :int;

    public function BgSprite (board :Board) :void
    {
        this.boardWidth = board.width;
        this.boardHeight = board.height;
        setupGraphics();
    }

    /**
     * Sets the center of the screen.  We need to adjust ourselves to match.
     */
    public function setAsCenter (boardX :Number, boardY :Number) :void
    {
        x = -(boardX*Codes.BG_PIXELS_PER_TILE);
        y = -(boardY*Codes.BG_PIXELS_PER_TILE);
    }

    /**
     * Draw the board.
     */
    public function setupGraphics () :void
    {
        // Our background, tiled if necessary.
        var tmpBmp :Bitmap = Bitmap(new spaceBg());

        var xRep :Number = Math.ceil((boardWidth*Codes.BG_PIXELS_PER_TILE +
                                         StarFight.WIDTH)/tmpBmp.width);
        var yRep :Number = Math.ceil((boardHeight*Codes.BG_PIXELS_PER_TILE +
                                         StarFight.HEIGHT)/tmpBmp.height);

        for (var x :int = 0; x < xRep; x++) {
            for (var y :int = 0; y < yRep; y++) {
                var bmp :Bitmap = Bitmap(new spaceBg());
                bmp.x = x*tmpBmp.width;
                bmp.y = y*tmpBmp.height;
                addChild(bmp);
            }
        }
    }

    /** Add in the spacey background image. */
    [Embed(source="rsrc/space_bg.png")]
    protected var spaceBg :Class;
}
}
