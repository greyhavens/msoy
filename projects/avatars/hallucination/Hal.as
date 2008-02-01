package {

import flash.display.BlendMode;
import flash.display.Loader;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.ProgressEvent;

import flash.filters.ColorMatrixFilter;

import flash.net.URLRequest;

import com.threerings.flash.FrameSprite;
import com.threerings.flash.Siner;

import com.whirled.AvatarControl;

[SWF(width="600", height="450")]
public class Hal extends FrameSprite
{
    public static const URL :String =
        "http://media.whirled.com/14e440599431ae0cb4bd04e38ec0b5cdbff38224.swf"; // Superstaler
        //"http://media.whirled.com/495b8868ecca0e0b06ad33c2f93f8bde11be8a01.swf"; // Fairy
        //"http://media.whirled.com/8ae8c90ab458b138b64f831383a826b505d60a79.swf"; // Lynn
        //"http://media.whirled.com/c95c59abc8da0ac99628fbc4c68799b93c129716.swf"; // Tofu

    public static const WIDTH :int = 600;
    public static const HEIGHT :int = 450;

    public static const SCALE :Number = 1;

    public function Hal ()
    {
        _ctrl = new AvatarControl(this);

        load(0, 1);
    }

    override protected function handleAdded (... ignored) :void
    {
        _dx.randomize();
        _dy.randomize();
        _scaler.randomize();
        super.handleAdded();
    }

    protected function load (from :int, to :int) :void
    {
        for (var ii :int = from; ii < to; ii++) {
            var loader :Loader = new Loader();
            loader.scaleX = SCALE;
            loader.scaleY = SCALE;
            _hals.push(loader);

            loader.contentLoaderInfo.sharedEvents.addEventListener(
                "controlConnect", handleConnect);

            // the first one gets a lot of special treatment
            if (ii == 0) {
                loader.contentLoaderInfo.addEventListener(ProgressEvent.PROGRESS, handleFrame);
                loader.contentLoaderInfo.addEventListener(Event.COMPLETE, handleFrame);
            }

            loader.load(new URLRequest(URL));

            // make them all have an alpha level
            loader.alpha = (COPIES - ii) / COPIES;
            loader.blendMode = BlendMode.LAYER;

            if (ii != 0) {
                loader.filters = [ new ColorMatrixFilter(COLOR_MATRICIES[ii - 1]) ];
            }

            addChildAt(loader, 0);
        }

        handleFrame();
    }

    override protected function handleFrame (... ignored) :void
    {
        var w :Number;
        var h :Number;
        try {
            w = _hals[0].contentLoaderInfo.width * SCALE;
            h = _hals[0].contentLoaderInfo.height * SCALE;

        } catch (err :Error) {
            return;
        }
        var dx :Number = DX + _dx.value;
        var dy :Number = DY + _dy.value;

        var baseX :Number = 0;
        if (isNaN(_orient) || _orient < 180) {
            dx *= -1;
            baseX = WIDTH - w;
        }

        var scale :Number = 1;

        var colorAdj :Number = 1 - Math.abs(_colorAdj.value);
 
        for (var ii :int = 0; ii < _hals.length; ii++) {
            var loader :Loader = Loader(_hals[ii]);
            loader.x = baseX + (dx * ii) + (w - (w * scale))/2;
            loader.y = (HEIGHT - h) - (dy * ii) + (h - (h * scale))/2;

            if (ii == 0) {
                scale -= Math.abs(_scaler.value);

            } else {
                loader.scaleX = SCALE * scale;
                loader.scaleY = SCALE * scale;
                scale = scale * scale;

                var matrix :Array = (COLOR_MATRICIES[ii - 1] as Array).concat();
                for (var jj :int = 0; jj < matrix.length; jj++) {
                    matrix[jj] = matrix[jj] * colorAdj;
                }
                loader.filters = [ new ColorMatrixFilter(matrix) ];
            }
        }

        _ctrl.setHotSpot(baseX + w/2, HEIGHT, h);
    }

    protected function handleConnect (cheese :Object) :void
    {
        var propName :String;
        var first :Boolean = (_halFuncs.length == 0);
        _halFuncs.push(cheese.userProps);

        if (first) {
            // replace all the hal's functions with interceptors
            var newProps :Object = {};
            for (propName in cheese.userProps) {
                newProps[propName] = createHalReplacement(propName, cheese.userProps[propName]);
            }
            cheese.userProps = newProps;

            // dispatch it upwards
            this.root.loaderInfo.sharedEvents.dispatchEvent((cheese as Event).clone());

            if (cheese.hostProps != null) {
                _whirledInitProps = clone(cheese.hostProps["initProps"]);

                _orient = _whirledInitProps["orient"];

                _whirledFuncs = cheese.hostProps;
                for (propName in _whirledFuncs) {
                    var fn :Function = (_whirledFuncs[propName] as Function);
                    if (fn != null) {
                        _whirledFuncs[propName] = createWhirledReplacement(propName, fn);
                    }
                }
            }
        }

        _whirledFuncs["initProps"] = clone(_whirledInitProps);
        cheese.hostProps = _whirledFuncs;

        // now, we can load the other ones
        if (first) {
            load(1, COPIES);
        }
    }

    protected function createHalReplacement (name :String, fn :Function) :Function
    {
        return function (... args) :* {
            // however we were called, we pass to all the others
            var result :*;

            if (name == "appearanceChanged_v1") {
                appearanceChanged_v1.apply(null, args);
            }

            for (var ii :int = _halFuncs.length - 1; ii >= 0; ii--) {
                var orig :Function = (_halFuncs[ii][name] as Function);
                result = orig.apply(null, args);
            }
            return result;
        };
    }

    protected function createWhirledReplacement (name :String, fn :Function) :Function
    {
        switch (name) {
        case "setHotSpot_v1":
            return function (... args) :* {
                // nada
            };

        default:
            return fn;
        }
    }

    protected function clone (obj :Object) :Object
    {
        var copy :Object = {};
        for (var prop :String in obj) {
            copy[prop] = obj[prop];
        }
        return copy;
    }

    protected function appearanceChanged_v1 (loc :Array, orient :Number, moving :Boolean) :void
    {
        _orient = orient;
    }

    protected var _ctrl :AvatarControl;

    protected var _hals :Array = [];

    protected var _halFuncs :Array = [];

    protected var _whirledFuncs :Object = {};

    protected var _whirledInitProps :Object = {};

    protected var _orient :Number;

    protected var _dx :Siner = new Siner(1, 2, 1.5, 1.9);

    protected var _dy :Siner = new Siner(1.5, 2.1, 1.5, 5);

    protected var _scaler :Siner = new Siner(.005, 5, .01, 9.1, .01, 2.9);

    protected var _colorAdj :Siner = new Siner(.12, .8, .12, 2.2);

    //protected var _whirledIntercepts :Object = {};

    protected static const COLOR_MATRICIES :Array = [
        [ 1, .25, .25, 0, 0,
          0, .75, 0, 0, 0,
          0, 0, .75, 0, 0,
          0, 0, 0, 1, 0 ],
        [ .75, 0, 0, 0, 0,
          .25, 1, .25, 0, 0,
          0, 0, .75, 0, 0,
          0, 0, 0, 1, 0 ],
        [ .75, 0, 0, 0, 0,
          0, .75, 0, 0, 0,
          .25, .25, 1, 0, 0,
          0, 0, 0, 1, 0 ],
        [ .5, 0, 0, 0, 0,
          .5, 1, .5, 0, 0,
          0, 0, .5, 0, 0,
          0, 0, 0, 1, 0 ],
        [ 1, .5, .5, 0, 0,
          0, .5, 0, 0, 0,
          0, 0, .5, 0, 0,
          0, 0, 0, 1, 0 ],
        [ .5, 0, 0, 0, 0,
          0, .5, 0, 0, 0,
          .5, .5, 1, 0, 0,
          0, 0, 0, 1, 0 ]
    ];

    protected static const COPIES :int = 7;

    protected static const DX :int = 15;
    protected static const DY :int = 15;
}
}
