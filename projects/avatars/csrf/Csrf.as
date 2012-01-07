//
// $Id$

package {

import flash.display.Sprite;
import flash.events.*;
import flash.net.*;

import com.threerings.util.Command;

[SWF(width="100", height="100")]
public class Csrf extends Sprite
{
    public function Csrf ()
    {
        var loader :URLLoader = new URLLoader();
        var request :URLRequest = new URLRequest();

        request.url = "http://www.whirled.com/usersvc";
        request.method = "POST";

        Command.bind(loader, Event.COMPLETE, function () :void {
            trace("Oh noes!! Successfully POSTed " + request.url +
                " and we're prone to CSRF attacks!");
        });

        loader.load(request);
    }
}
}
