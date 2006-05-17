package {

import flash.display.Sprite;

import flash.events.Event;
import flash.events.TextEvent;

import flash.net.LocalConnection;

public class AvatarTest extends Sprite
{
    public function AvatarTest ()
    {
        loaderInfo.addEventListener(Event.INIT, loadedComplete);

        loaderInfo.sharedEvents.addEventListener(
            "setLook", setLookEvent);

        setLook();
    }

    public function setLookEvent (event :TextEvent) :void
    {
        trace("ignoring setLook(" + event + ")");
//        setLook(event.text);
    }

    public function setLook (style :String = null) :void
    {
        with (graphics) {
            clear();
            if (style === "red") {
                beginFill(0xFF0000); // red

            } else {
                beginFill(0x0000FF); // blue
            }

            moveTo(0, 0);
            lineTo(30, 30);
            lineTo(-30, 30);
            lineTo(30, -30);
            lineTo(-30, -30);
            lineTo(0, 0);
            endFill();
        }
    }

    protected function loadedComplete (event :Event) :void
    {
        // remove the listeneer
        loaderInfo.removeEventListener(Event.INIT, loadedComplete);

        var o :Object = loaderInfo.parameters;
        var id :String = "_msoy" + o["oid"];

        // NOTE: an essential part of this is that a reference must be kept
        // to the LocalConnection, or it will auto-close.
        var c :LocalConnection = new LocalConnection();
        _lc = c;
        c.allowDomain("*");
        c.client = new Object();
        c.client.setLook = setLook;

        try {
            c.connect(id);
            trace("A) opened named connection \"" + id + "\" at domain \"" +
                c.domain + "\".");

        } catch (e :Error) {
            trace("couldn't connect msoy: " + e);
        }

        // this will lock up the entire browser
//        while (true) { }
    }

    protected var _lc :LocalConnection;
}
}
