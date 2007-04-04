package {

import flash.display.DisplayObject;
import flash.display.Sprite;

import com.whirled.AvatarControl;
import com.whirled.ControlEvent;

[SWF(width="151", height="150")]
public class Avatar extends Sprite
{
    /**
     * Constructor.
     */
    public function Avatar ()
    {
        // create and add the image
        _image = (new IMAGE() as DisplayObject);
        addChild(_image);

        // create the control for whirled communication
        _control = new AvatarControl(this);
        // add a listener for appearance updates
        _control.addEventListener(ControlEvent.APPEARANCE_CHANGED,
            handleAppearanceChanged);

        // and kick things off by updating the appearance immediately
        updateAppearance();
    }

    /**
     * Handles ControlEvent.APPEARANCE_CHANGED.
     */
    protected function handleAppearanceChanged (event :ControlEvent) :void
    {
        updateAppearance();
    }

    /**
     * Update the appearance of this avatar.
     */
    protected function updateAppearance () :void
    {
        var orientation :Number = _control.getOrientation();

        // we assume that our image naturally faces left
        if (orientation < 180) {
            // we need to face right
            _image.x = _image.width;
            _image.scaleX = -1;

        } else {
            // normal facing
            _image.x = 0;
            _image.scaleX = 1;
        }
    }

    /** The image we're displaying. */
    protected var _image :DisplayObject;

    /** The avatar control interface. */
    protected var _control :AvatarControl;

    /** The embedded image class. */
    [Embed(source="Rooster.png")]
    protected static const IMAGE :Class;
}

} // END: package
