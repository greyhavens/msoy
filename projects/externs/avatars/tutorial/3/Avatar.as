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

        // add custom actions and listen for custom action events
        _control.setActions("Beep", "Double Beep");
        _control.addEventListener(ControlEvent.ACTION_TRIGGERED,
            handleAction);

        // create our beep sound
        _beep = (new BEEP() as Sound);

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

    /**
     * Called just prior to every frame that is rendered to screen.
     */
    protected function handleEnterFrame (event :Event) :void
    {
        var now :Number = getTimer();
        var elapsed :Number = getTimer() - _bounceStamp;

        // compute our bounce
        _image.y = BOUNCE_AMPLITUDE *
            Math.sin(elapsed * (Math.PI * 2) / BOUNCE_PERIOD);
    }

    /**
     * Handle ACTION_TRIGGERED to play our custom actions.
     */
    protected function handleAction (event :ControlEvent) :void
    {
        switch (event.name) {
        case "Beep":
            _beep.play();
            break;

        case "Double Beep":
            _beep.play(0, 2);
            break;

        default:
            trace("Unknown action: " + event.name);
            break;
        }
    }

    /** The image we're displaying. */
    protected var _image :DisplayObject;

    /** Our beep sound. */
    protected var _beep :Sound;

    /** The avatar control interface. */
    protected var _control :AvatarControl;

    /** Controls the timing of our speak action. */
    protected var _speakTimer :Timer;

    /** Are we bouncing? */
    protected var _bouncing :Boolean;

    /** The timestamp at which we started bouncing. */
    protected var _bounceStamp :Number;

    /** The amplitude of our bounce, in pixels. */
    protected static const BOUNCE_AMPLITUDE :int = 10;

    /** The period of our bounce: we do one bounce every 500 milliseconds. */
    protected static const BOUNCE_PERIOD :int = 500;

    /** The embedded image class. */
    [Embed(source="Rooster.png")]
    protected static const IMAGE :Class;

    [Embed(source="beep.mp3")]
    protected static const BEEP :Class;
}

} // END: package
