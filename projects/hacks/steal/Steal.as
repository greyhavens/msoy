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
    public static const REDIRS :Array = 
        [ "http://bogocorp.com/~ray/steal-1.txt",
          "http://bogocorp.com/~ray/steal-2.txt" ];

    public static const ACTION_PREFIX :String = "stealer ";

    public function Steal ()
    {
        _host = new Loader();
        addChild(_host);

        // set up redirects for the metasoy messages
        _host.contentLoaderInfo.sharedEvents.addEventListener(
            "controlConnect", controlPassThru);

        loadStolen(0);
    }

    /**
     * Load the stolen avatar specified.
     */
    protected function loadStolen (index :int) :void
    {
        // look up the media that we should be showing
        var loader :URLLoader =
            new URLLoader(new URLRequest(String(REDIRS[index])));
        loader.addEventListener(Event.COMPLETE, function (event :Event) :void {
            _host.load(new URLRequest(String(loader.data)));
        });
    }

    /**
     * Shuttle messages from our stolen content up to metasoy.
     */
    protected function controlPassThru (evt :Event) :void
    {
        var userProps :Object = (evt as Object)["userProps"];

        if ("getActions_v1" in userProps) {
            var origGet :Function = (userProps["getActions_v1"] as Function);
            if (origGet != null) {
                userProps["getActions_v1"] = function () :Array {
                    var array :Array = (origGet() as Array);
                    if (array != null) {
                        for (propName in REDIRS) {
                            array.unshift(ACTION_PREFIX + propName);
                        }
                    }
                    return array;
                };
            }
        }
        if ("action_v1" in userProps) {
            var origDo :Function = (userProps["action_v1"] as Function);
            if (origDo != null) {
                userProps["action_v1"] = function (action :String) :void {
                    if (action.indexOf(ACTION_PREFIX) == 0) {
                        loadStolen(
                            parseInt(action.substring(ACTION_PREFIX.length)));
                    } else {
                        origDo(action);
                    }
                };
            }
        }

        // dispatch it upwards
        this.root.loaderInfo.sharedEvents.dispatchEvent(evt.clone());
    }

    /** Holds our stolen content. */
    protected var _host :Loader = new Loader();
}
}
