//
// $Id$

package {

import flash.display.DisplayObject;
import flash.display.Sprite;

import com.threerings.msoy.export.AvatarControl;

[SWF(width="161", height="150")]
public class ImageFlipper extends Sprite
{
    public function ImageFlipper ()
    {
        var o :Object = new Data().content;
        var imgClass :Class = (o["image"] as Class);
        _image = (new imgClass() as DisplayObject);
        addChild(_image);

        _control = new AvatarControl(this);
        _control.appearanceChanged = setupVisual;
        setupVisual();
    }

    protected function setupVisual () :void
    {
        var orient :Number = _control.getOrientation();
        if (orient < 180) {
            _image.x = _image.width;
            _image.scaleX = -1;

        } else {
            _image.x = 0;
            _image.scaleX = 1;
        }
    }

    protected var _image :DisplayObject;
    protected var _control :AvatarControl;
}
}
