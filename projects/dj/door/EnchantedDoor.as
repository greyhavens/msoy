//
// $Id$
//
// Treasure Chest - a piece of furni for Whirled

package {

import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.display.DisplayObject;
import flash.display.MovieClip;

import flash.geom.ColorTransform;

import com.whirled.FurniControl;
import com.whirled.ControlEvent;
import com.whirled.EntityControl;
import com.whirled.DataPack;

/**
 * Treasure Chest is the coolest piece of Furni ever.
 */
[SWF(width="200", height="300")]
public class EnchantedDoor extends Sprite
{
    public static const OPEN_DOOR_SIGNAL :String = "dj_tutorial_complete";

    public function EnchantedDoor ()
    {
        // instantiate and wire up our control
        _control = new FurniControl(this);

        // listen for an unload event
        _control.addEventListener(Event.UNLOAD, handleUnload);
        DataPack.load(_control.getDefaultDataPack(), gotPack);
    }

    protected function gotPack (result:Object) : void
    {
        if (result is DataPack) {
            speed = result.getNumber("TransitionSpeed");
            result.getDisplayObjects({open:"DoorImage", shadow:"DoorImage"}, gotDisplayObject);
        } else {
            speed = 10;
            gotDisplayObject(null);
        }
    }

    protected function gotDisplayObject(result:Object) : void {
        if (result) {
            openImage = result.open;
            shadowLayer = result.shadow;
        } else {
            openImage = new OPEN() as DisplayObject;
            shadowLayer = new OPEN() as DisplayObject;
        }
        openImage.y = 300 - openImage.height;
        openImage.x = (200 - openImage.width) / 2;
        openImage.alpha = 0;
        shadowLayer.y = 300 - openImage.height;
        shadowLayer.x = (200 - openImage.width) / 2;

        var transform:ColorTransform = new ColorTransform;
        transform.color = 0x000000;
        shadowLayer.transform.colorTransform = transform;
        shadowLayer.alpha = .5;


        maskLayer = new MASK() as DisplayObject;
        maskLayer.width = openImage.width;
        maskLayer.height = openImage.height;
        maskLayer.x = openImage.x;
        maskLayer.y = openImage.y;

        doorContainer = new Sprite;
        doorContainer.addChild(shadowLayer);
        doorContainer.addChild(maskLayer);
        shadowLayer.mask = maskLayer;

        shadowLayer.y = maskLayer.y + maskLayer.height;

        addChild(doorContainer);
        //addChild(openImage);

        state = _control.getMemory("state", "closed") as String;
        if (state == "closed") {
            // Do nothing
        } else if (state == "open") {
            handleMsgReceived({name:"open"});
        } else {
            trace("[EnchantedDoor] Error: Invalid state::" + state);
        }

        _control.addEventListener(ControlEvent.MESSAGE_RECEIVED, handleMsgReceived);
        _control.addEventListener(ControlEvent.SIGNAL_RECEIVED, handleSignal);
    }

    protected function handleMsgReceived(event: Object) : void
    {
        switch (event.name) {
            case "open":
                state = "open";
                _control.setMemory("state", state);

                addEventListener(Event.ENTER_FRAME, frameHandler);

                break;
            case "close":
                state = "closed";
                _control.setMemory("state", "closed");

                addEventListener(Event.ENTER_FRAME, frameHandler);
            break;
        }
    }

    protected function handleSignal(event:ControlEvent) : void {
        if (event.name == OPEN_DOOR_SIGNAL) {
	    if (state == "closed") {
                _control.sendMessage("open");
            }
        }

    }

    protected function frameHandler(event:Event) : void {
        if (state == "open") {
            shadowLayer.y -= speed;
            if (shadowLayer.y <= maskLayer.y) {
                // Done Raising Shadow
                shadowLayer.y = maskLayer.y

                if (!this.contains(openImage)) {
                    this.addChild(openImage);
                }

                openImage.alpha += speed * .025;
                if (openImage.alpha >= 1) {
                    openImage.alpha = 1;
                    removeEventListener(Event.ENTER_FRAME, frameHandler);
                }
            }
        } else if (state == "closed") {
            openImage.alpha -= speed * .025;
            if (openImage.alpha <= 0) {
                openImage.alpha = 0;

                if (this.contains(openImage)) {
                    this.removeChild(openImage);
                }

                shadowLayer.y += speed;
                if (shadowLayer.y >= maskLayer.y + maskLayer.height) {
                    // Done closing
                    shadowLayer.y = maskLayer.y + maskLayer.height;
                    removeEventListener(Event.ENTER_FRAME, frameHandler);
                }
            }
        }

    }

    /**
     * This is called when your furni is unloaded.
     */
    protected function handleUnload (event :Event) :void
    {
        // stop any sounds, clean up any resources that need it.  This specifically includes
        // unregistering listeners to any events - especially Event.ENTER_FRAME
        if (hasEventListener(Event.ENTER_FRAME)) {
            removeEventListener(Event.ENTER_FRAME, frameHandler);
        }

    }

    protected var openImage:*;
    protected var closedImage:*;
    protected var state:String;
    protected var speed:int = 10;

    protected var _control :FurniControl;

    protected var doorContainer:Sprite;
    protected var maskLayer:DisplayObject;
    protected var shadowLayer:*;

    [Embed(source="door.swf")]
    protected static const OPEN:Class;
    [Embed(source="mask.png")]
    protected static const MASK:Class;
}
}
