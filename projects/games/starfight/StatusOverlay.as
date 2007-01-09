package {

import flash.display.Sprite;
import flash.display.Shape;
import flash.display.Bitmap;

public class StatusOverlay extends Sprite
{
    public function StatusOverlay() :void
    {
        addChild(_power = new Sprite());
        _power.graphics.beginFill(Codes.CYAN);
        _power.graphics.drawRoundRect(0, 0, POW_WIDTH, POW_HEIGHT, 2.0, 2.0);
        _power.x = 697;
        _power.y = 38;
        var mask :Shape = new Shape();
        mask.graphics.beginFill(0xFFFFFF);
        mask.graphics.drawRect(0, 0, POW_WIDTH, POW_HEIGHT);
        _power.addChild(mask);
        _power.mask = mask;

        addChild(Bitmap(new frameAsset()));
        addChild(_spread = (Bitmap(new spreadAsset())));
        _spread.x = 696;
        _spread.y = 63;
        addChild(_speed = (Bitmap(new speedAsset())));
        _speed.x = 727;
        _speed.y = 63;
        addChild(_shields = (Bitmap(new shieldsAsset())));
        _shields.x = 758;
        _shields.y = 63;
    }

    /**
     * Shows the powerups held by the ship.
     */
    public function setPowerups(powerups :int) :void
    {
        _speed.alpha = ((powerups & ShipSprite.SPEED_MASK) ? 1.0 : 0.0);
        _spread.alpha = ((powerups & ShipSprite.SPREAD_MASK) ? 1.0 : 0.0);
        _shields.alpha = ((powerups & ShipSprite.SHIELDS_MASK) ? 1.0 : 0.0);
    }

    /**
     * Sets our power level.
     */
    public function setPower(power :Number) :void
    {
        var mask :Shape = Shape(_power.mask);
        mask.graphics.clear();
        mask.graphics.beginFill(0xFFFFFF);
        mask.graphics.drawRect(0, 0, POW_WIDTH*power, POW_HEIGHT);
        mask.graphics.endFill();
    }

    [Embed(source="rsrc/status.png")]
    protected var frameAsset :Class;

    [Embed(source="rsrc/spread.png")]
    protected var spreadAsset :Class;

    [Embed(source="rsrc/speed.png")]
    protected var speedAsset :Class;

    [Embed(source="rsrc/shields.png")]
    protected var shieldsAsset :Class;

    /** Powerup bitmaps. */
    protected var _speed :Bitmap;
    protected var _spread :Bitmap;
    protected var _shields :Bitmap;

    /** HP bar. */
    protected var _power :Sprite;


    protected static const POW_WIDTH :int = 85;
    protected static const POW_HEIGHT :int = 8;
}
}
