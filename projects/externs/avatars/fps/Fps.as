package {

import flash.display.Sprite;

import com.threerings.flash.FPSDisplay;

[SWF(width="63", height="20")]
public class Fps extends Sprite
{
    public function Fps ()
    {
//        var f :FPSDisplay = new FPSDisplay();
//        trace("f: " + f.width + " , " + f.height);
        addChild(new FPSDisplay());
    }
}
}
