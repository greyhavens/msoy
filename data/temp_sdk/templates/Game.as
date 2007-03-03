//
// $Id$
//
// @project@ - a game for Whirled

package {

import flash.display.Sprite;

import com.whirled.WhirledGameControl;

[SWF(width="400", height="400")]
public class @project@ extends Sprite
{
    public function @project@ ()
    {
        _control = new WhirledGameControl(this);
    }

    protected var _control :WhirledGameControl;
}
}
