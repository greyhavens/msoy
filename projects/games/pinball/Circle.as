package {

import org.cove.flade.surfaces.CircleTile;
import org.cove.flade.graphics.GfxUtil;
import mx.core.SoundAsset;

public class Circle extends CircleTile
{
    public var sound :SoundAsset;
    public var color :uint;

    public function Circle(cx:Number, cy:Number, r:Number)
    {
        super(cx, cy, r);
    }

    override public function paint():void {
        if (isVisible) {
            graphics.clear();
            GfxUtil.paintCircle(graphics, center.x, center.y, radius, color);
        }
    }
}

}