//
// $Id$

package {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.ProgressEvent;

import flash.net.URLRequest;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.SecurityDomain;

[SWF(width="600", height="450")]
public class Inverter extends Sprite
{
    //public static const URL :String = "http://media.whirled.com/c95c59abc8da0ac99628fbc4c68799b93c129716.swf";
    public static const URL :String = "http://media.whirled.com/14e440599431ae0cb4bd04e38ec0b5cdbff38224.swf"

    public static const WIDTH :int = 600;
    public static const HEIGHT :int = 450;

    public static const CONTENT_WIDTH :int = 600;
    public static const CONTENT_HEIGHT :int = 450;

    public function Inverter ()
    {
        _loader = new Loader();
        addChild(_loader);

        _loader.contentLoaderInfo.sharedEvents.addEventListener("controlConnect", controlPassThru);

        _loader.contentLoaderInfo.addEventListener(ProgressEvent.PROGRESS, checkAlignment);
        _loader.contentLoaderInfo.addEventListener(Event.COMPLETE, checkAlignment);
        _loader.load(new URLRequest(URL),
            //new LoaderContext(true, ApplicationDomain.currentDomain, SecurityDomain.currentDomain));
            new LoaderContext(false, new ApplicationDomain(null), null));
    }

    /**
     * This is called when your avatar's orientation changes or when it transitions from not
     * walking to walking and vice versa.
     */
    protected function checkAlignment (... ignored) :void
    {
        var w :Number;
        var h :Number;
        try {
            w = _loader.contentLoaderInfo.width;
            h = _loader.contentLoaderInfo.height;

        } catch (err :Error) {
            w = CONTENT_WIDTH;
            h = CONTENT_HEIGHT;
        }

        var normal :Boolean = (_ourState == NORMAL);
        _loader.x = (WIDTH - w) / 2;
        _loader.y = normal ? 0 : h;
        _loader.scaleY = normal ? 1 : -1;
        if (_setPreferredY != null) {
            _setPreferredY(normal ? 0 : 10000);
            _setHotSpot(WIDTH / 2, normal ? h : 0, normal ? NaN : -h);
        }
    }

    protected function controlPassThru (evt :Object) :void
    {
        var userProps :Object = evt.userProps;
        replaceProp(userProps, "getStates_v1", function (orig :Function) :Function {
            return function () :Array {
                return StateMultiplexor.createStates(orig(), STATES);
            };
        });
        replaceProp(userProps, "stateSet_v1", function (orig :Function) :Function {
            return function (newState :String) :void {
                orig(StateMultiplexor.getState(newState, 0));
                setState(newState);
                checkAlignment();
            }
        });
        replaceProp(userProps, "appearanceChanged_v1", function (orig :Function) :Function {
            return function (loc :Array, orient :Number, moving :Boolean) :void {
                _loc = loc;
                orig(loc, orient, moving);
            }
        });

        // dispatch it upwards
        this.root.loaderInfo.sharedEvents.dispatchEvent((evt as Event).clone());

        var hostProps :Object = evt.hostProps;
        if (hostProps == null) {
            return;
        }
        replaceProp(hostProps, "getState_v1", function (orig :Function) :Function {
            // set up our current shite
            setState(orig());

            return function () :String {
                return StateMultiplexor.getState(orig(), 0);
            }
        });

        _setPreferredY = hostProps["setPreferredY_v1"];
        _setHotSpot = hostProps["setHotSpot_v1"];
        _setLoc = hostProps["setLocation_v1"];

        _loc = hostProps["initProps"]["location"];
        checkAlignment();
    }

    protected function replaceProp (props :Object, propName :String, replacer :Function) :void
    {
        // so fucking loosy-goosy
        if (props != null && propName in props) {
            props[propName] = replacer(props[propName]);
        }
    }

    protected function setState (fullState :String) :void
    {
        var newState :int;
        if (fullState == null) {
            newState = NORMAL;

        } else {
            newState = Math.max(0, STATES.indexOf(StateMultiplexor.getState(fullState, 1)));
        }
        if (newState != _ourState) {
            _ourState = newState;

            // and change our location automatically
            if (_setLoc != null) {
                _setLoc(_loc[0], (_ourState == NORMAL) ? 0 : 1, _loc[2], 0);
            }
        }
    }

    protected var _loader :Loader;

    protected var _ourState :int = NORMAL;

    protected var _loc :Array;

    protected var _setPreferredY :Function;
    protected var _setHotSpot :Function;
    protected var _setLoc :Function;

    protected static const NORMAL :int = 0;
    protected static const INVERTED :int = 1;

    protected static const STATES :Array = [ "Normal", "Inverted" ];
}
}
