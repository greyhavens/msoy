package {

import flash.display.Loader;
import flash.display.Sprite;

import flash.events.Event;

import flash.net.URLLoader;
import flash.net.URLRequest;

[SWF(width="1", height="1")]
public class Steal extends Sprite
{
    /** The url we query to identify the stolen media to use. */
    public static const REDIR :String = "http://bogocorp.com/~ray/steal-1.txt";

    public function Steal ()
    {
        _host = new Loader();
        addChild(_host);

        // set up redirects for the metasoy messages
        this.root.loaderInfo.sharedEvents.addEventListener(
            "msoyMessage", handleMessage);
        _host.contentLoaderInfo.sharedEvents.addEventListener(
            "msoyQuery", handleQuery);

        // look up the media that we should be showing
        var loader :URLLoader = new URLLoader(new URLRequest(REDIR));
        loader.addEventListener(Event.COMPLETE, function (event :Event) :void {
            _host.load(new URLRequest(String(loader.data)));
        });
    }

    /**
     * Shuttle messages from metasoy down to our stolen content.
     */
    protected function handleMessage (evt :Event) :void
    {
        var copy :Event = evt.clone();
        var copyO :Object = copy;
        var evtO :Object = evt;

        copyO.msoyName = evtO.msoyName;
        copyO.msoyValue = evtO.msoyValue;
        // dispatch it downwards
        _host.contentLoaderInfo.sharedEvents.dispatchEvent(copy);
        evtO.msoyResponse = copyO.msoyResponse;
    }

    /**
     * Shuttle messages from our stolen content up to metasoy.
     */
    protected function handleQuery (evt :Event) :void
    {
        var copy :Event = evt.clone();
        var copyO :Object = copy;
        var evtO :Object = evt;

        copyO.msoyName = evtO.msoyName;
        copyO.msoyValue = evtO.msoyValue;
        // dispatch it upwards
        this.root.loaderInfo.sharedEvents.dispatchEvent(copy);
        evtO.msoyResponse = copyO.msoyResponse;
    }

    /** Holds our stolen content. */
    protected var _host :Loader = new Loader();
}
}
