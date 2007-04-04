package {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.events.TimerEvent;

import flash.filters.GlowFilter;

import flash.utils.Timer;

import com.whirled.AvatarControl;
import com.whirled.ControlEvent;

[SWF(width="181", height="170")]
public class Avatar extends Sprite
{
    /**
     * Constructor.
     */
    public function Avatar ()
    {
        // create and add the image
        _image = (new IMAGE() as DisplayObject);
        // use the imageholder sprite to easily offset the image without
        // having to worry about the image's offset ever again
        var imageHolder :Sprite = new Sprite();
        imageHolder.x = 10;
        imageHolder.y = 10;
        imageHolder.addChild(_image);
        addChild(imageHolder);

        // create the control for whirled communication
        _control = new AvatarControl(this);
        // add a listener for appearance updates
        _control.addEventListener(ControlEvent.APPEARANCE_CHANGED,
            handleAppearanceChanged);
        // add a listener to hear about speak events
        _control.addEventListener(ControlEvent.AVATAR_SPOKE,
            handleSpoke);

        // When we do speak, we're going to glow purple for 200ms, then stop
        _speakTimer = new Timer(200, 1);
        _speakTimer.addEventListener(TimerEvent.TIMER, doneSpeaking);

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

    /**
     * We've just spoken. Glow purple for a bit.
     */
    protected function handleSpoke (event :ControlEvent) :void
    {
        // reset the timer, cancels any pending 'doneSpeaking' call.
        _speakTimer.reset();

        // only set up the filter if it hasn't already
        if (this.filters == null || this.filters.length == 0) {
            this.filters = [ new GlowFilter(0xFF00FF, 1, 10, 10) ];
        }
        // start the timer
        _speakTimer.start();
    }

    /**
     * It's time to stop showing our speaking glow.
     */
    protected function doneSpeaking (event :TimerEvent) :void
    {
        this.filters = null;
    }

    /** The image we're displaying. */
    protected var _image :DisplayObject;

    /** The avatar control interface. */
    protected var _control :AvatarControl;

    /** Controls the timing of our speak action. */
    protected var _speakTimer :Timer;

    /** The embedded image class. */
    [Embed(source="Rooster.png")]
    protected static const IMAGE :Class;
}

} // END: package
