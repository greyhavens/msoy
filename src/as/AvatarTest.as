package {

import flash.display.Sprite;

import flash.net.LocalConnection;

import flash.util.trace;

public class AvatarTest extends Sprite
{
    public function AvatarTest ()
    {
        setLook();

        var c :LocalConnection = new LocalConnection();
        c.client = new Object();
        c.client.setLook = setLook;
        trace("avatartest domain: " + c.domain);
        try {
            c.connect("_msoy");
        } catch (e :Error) {
            trace("couldn't connect msoy: " + e);
        }
    }

    public function setLook (style :String = null) :void
    {
        graphics.clear();
        if (style === "red") {
            graphics.beginFill(0xFF0000); // red

        } else {
            graphics.beginFill(0x0000FF); // blue
        }

        graphics.moveTo(0, 0);
        graphics.lineTo(30, 30);
        graphics.lineTo(-30, 30);
        graphics.lineTo(30, -30);
        graphics.lineTo(-30, -30);
        graphics.lineTo(0, 0);
        graphics.endFill();
    }
}
}
