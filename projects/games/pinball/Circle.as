package {

import org.cove.flade.surfaces.CircleTile;
import mx.core.SoundAsset;

public class Circle extends CircleTile
{
    public var sound :SoundAsset;

    public function Circle(cx:Number, cy:Number, r:Number)
    {
        super(cx, cy, r);
    }
}

}