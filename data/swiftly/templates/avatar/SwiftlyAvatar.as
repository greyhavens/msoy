//
// $Id$
//
// SwiftlyAvatar - an avatar for Whirled

package {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.filters.GlowFilter;

import flash.media.Sound;

import flash.utils.getTimer; // function import
import flash.utils.Timer;

import com.whirled.AvatarControl;
import com.whirled.ControlEvent;

[SWF(width="333", height="432")]
public class SwiftlyAvatar extends Sprite
{
    /** An action users can select on our avatar. */
    public static const GLOW_AQUA :String = "Glow aqua";

    /** An action users can select on our avatar. */
    public static const GLOW_YELLOW :String = "Glow yellow";

    /**
     * Creates and initializes the avatar.
     */
    public function SwiftlyAvatar ()
    {
        // create and add the image
        _image = (new IMAGE() as DisplayObject);
        addChild(_image);

        // create the control for whirled communication
        _control = new AvatarControl(this);
        // add a listener for appearance updates
        _control.addEventListener(ControlEvent.APPEARANCE_CHANGED, handleAppearanceChanged);
        // add a listener to hear about speak events
        _control.addEventListener(ControlEvent.AVATAR_SPOKE, handleSpoke);

        // add custom actions and listen for custom action events
        _control.registerActions(GLOW_AQUA, GLOW_YELLOW);
        _control.addEventListener(ControlEvent.ACTION_TRIGGERED, handleAction);

        // When we do speak, we're going to glow purple for 200ms, then stop
        _glowTimer = new Timer(200, 1);
        _glowTimer.addEventListener(TimerEvent.TIMER, doneGlowing);

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
            // Set our hotspot to where our "feet" are in the image
            _control.setHotSpot(_image.width-140, 290);

        } else {
            // normal facing
            _image.x = 0;
            _image.scaleX = 1;
            // Set our hotspot to where our "feet" are in the image
            _control.setHotSpot(140, 290);
        }

        // see if we need to update our bouncing
        if (_bouncing != _control.isMoving()) {
            _bouncing = _control.isMoving();
            if (_bouncing) {
                addEventListener(Event.ENTER_FRAME, handleEnterFrame);
                _bounceStamp = getTimer();

            } else {
                removeEventListener(Event.ENTER_FRAME, handleEnterFrame);
                _image.y = 0; // reset to unbouncing
            }
        }
    }

    /**
     * We've just spoken. Glow purple for a bit.
     */
    protected function handleSpoke (event :ControlEvent) :void
    {
        startGlowing(0xFF00FF);
    }

    /**
     * Glows our avatar with the specified color for a short time.
     */
    protected function startGlowing (color :uint) :void
    {
        // reset the timer, cancels any pending 'doneGlowing' call.
        _glowTimer.reset();

        // only set up the filter if we're not in the middle of glowing already
        if (this.filters == null || this.filters.length == 0) {
            this.filters = [ new GlowFilter(color, 1, 10, 10) ];
        }

        // start the timer
        _glowTimer.start();
    }

    /**
     * It's time to stop showing our glow.
     */
    protected function doneGlowing (event :TimerEvent) :void
    {
        this.filters = null;
    }

    /**
     * Called just prior to every frame that is rendered to screen.
     */
    protected function handleEnterFrame (event :Event) :void
    {
        var now :Number = getTimer();
        var elapsed :Number = getTimer() - _bounceStamp;

        // compute our bounce
        _image.y = -Math.abs(BOUNCE_AMPLITUDE * Math.sin(elapsed * Math.PI / BOUNCE_PERIOD));
    }

    /**
     * Handle ACTION_TRIGGERED to play our custom actions.
     */
    protected function handleAction (event :ControlEvent) :void
    {
        switch (event.name) {
        case GLOW_AQUA:
            startGlowing(0x00FFFF);
            break;

        case GLOW_YELLOW:
            startGlowing(0xFFFF00);
            break;

        default:
            trace("Unknown action: " + event.name);
            break;
        }
    }

    /** The image we're displaying. */
    protected var _image :DisplayObject;

    /** The avatar control interface. */
    protected var _control :AvatarControl;

    /** Controls the timing of our glowing. */
    protected var _glowTimer :Timer;

    /** Are we bouncing? */
    protected var _bouncing :Boolean;

    /** The timestamp at which we started bouncing. */
    protected var _bounceStamp :Number;

    /** The amplitude of our bounce, in pixels. */
    protected static const BOUNCE_AMPLITUDE :int = 10;

    /** The period of our bounce: we do one bounce every 300 milliseconds. */
    protected static const BOUNCE_PERIOD :int = 300;

    /** Our avatar image. */
    [Embed(source="avatar.png")]
    protected static const IMAGE :Class;
}
}
