//
// $Id$

package {

import flash.display.Sprite
import flash.display.Shape;

public class Shooter extends Sprite
{
    public function Shooter (pidx :int)
    {
        var square :Shape = new Shape();
        square.graphics.beginFill(Content.SHOOTER_COLOR[pidx]);
        // square.graphics.lineStyle(borderSize, borderColor);
        square.graphics.moveTo(0, -Content.SHOOTER_SIZE/2);
        square.graphics.lineTo(0, Content.SHOOTER_SIZE/2);
        square.graphics.lineTo(Content.SHOOTER_SIZE/2, 0);
        square.graphics.lineTo(0, -Content.SHOOTER_SIZE/2);
        square.graphics.endFill();
        addChild(square);

        rotation = SHOOTER_ORIENT[pidx];
    }

    protected static const SHOOTER_ORIENT :Array = [ 270, 90, 0, 180 ];
}

}
