//
// $Id$
//
// @project@ - an avatar for Whirled

package {

import flash.display.Sprite;

import flash.events.Event;

import com.whirled.AvatarControl;
import com.whirled.ControlEvent;

[SWF(width="50", height="50")]
public class @project@ extends Sprite
{
    public function @project@ ()
    {
        // listen for an unload event
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        _control = new AvatarControl(this);

        // Uncomment this to be notified when your avatar changes orientation
        // _control.addEventListener(ControlEvent.APPEARANCE_CHANGED, appearanceChanged);

        // Uncomment this to be notified when the player speaks
        // _control.addEventListener(ControlEvent.AVATAR_SPOKE, avatarSpoke);

        // Uncomment this to export custom avatar actions
        // _control.addEventListener(ControlEvent.ACTION_TRIGGERED, handleAction);
        // _control.setActions("Test action");

        appearanceChanged();
    }

    /**
     * This is called when your avatar's orientation changes or when it transitions from not
     * walking to walking and vice versa.
     */
    protected function appearanceChanged (event :Object = null) :void
    {
        var orient :Number = _control.getOrientation();
        var walking :Boolean = _control.isMoving();

        // Draw your avatar here using the appropriate orientation and accounting for whether it is
        // walking
    }

    /**
     * This is called when your avatar speaks.
     */
    protected function avatarSpoke (event :Object = null) :void
    {
    }

    /**
     * This is called when the user selects a custom action exported on your avatar or when any
     * other trigger event is received.
     */
    protected function handleAction (event :ControlEvent) :void
    {
    }

    /**
     * This is called when your avatar is unloaded.
     */
    protected function handleUnload (event :Event) :void
    {
        // stop any sounds, clean up any resources that need it.  This specifically includes 
        // unregistering listeners to any events - especially Event.ENTER_FRAME
    }

    protected var _control :AvatarControl;
}
}
