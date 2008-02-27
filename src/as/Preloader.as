//
// $Id$

package {

import flash.display.DisplayObjectContainer;
import flash.display.Graphics;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Sprite;
import flash.display.StageScaleMode;

import flash.events.Event;
import flash.events.ProgressEvent;

import flash.net.URLRequest;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;

import com.threerings.util.ParameterUtil;

[SWF(width="200", height="200")]
public class Preloader extends Sprite
{
    public function Preloader ()
    {
        stage.scaleMode = StageScaleMode.NO_SCALE;

        drawProgress(0, 100);

        ParameterUtil.getParameters(this, gotParams);
    }

    protected function gotParams (params :Object) :void
    {
        var toload :String = params["toload"];

        var loader :Loader = new Loader();
        loader.contentLoaderInfo.addEventListener(Event.INIT, handleInit);
        loader.contentLoaderInfo.addEventListener(ProgressEvent.PROGRESS, handleProgress);
        loader.contentLoaderInfo.addEventListener(Event.COMPLETE, handleComplete);
        loader.load(new URLRequest(toload), new LoaderContext(false, new ApplicationDomain(null)));
        _loader = loader;
    }

    protected function handleInit (event :Event) :void
    {
        trace(":: Init!");
    }

    protected function handleProgress (event :ProgressEvent) :void
    {
        drawProgress(event.bytesLoaded, event.bytesTotal);
    }

    protected function handleComplete (event :Event) :void
    {
        trace(":: Complete!");

        addEventListener(Event.ENTER_FRAME, handleOnceFrame);
    }

    protected function handleOnceFrame (event :Event) :void
    {
        removeEventListener(Event.ENTER_FRAME, handleOnceFrame);

        var parent :DisplayObjectContainer = this.parent;
        parent.removeChild(this);
        parent.addChild(_loader.content);
        _loader = null;

//        try {
//            loader.unload();
//        } catch (err :Error) {
//            // ignore
//        }
    }

    protected function drawProgress (part :Number, whole :Number) :void
    {
        var y :int = (stage.stageHeight - BAR_HEIGHT) / 2;

        var perc :Number = part / whole;
        trace("Progress: " + (perc * 100).toFixed(2));

        var g :Graphics = graphics;
        g.clear();
        g.lineStyle(1, 0xFFFFFF);
        g.drawRect(0, y, stage.stageWidth, BAR_HEIGHT);
        g.lineStyle(0, 0, 0);
        g.beginFill(0x009900);
        g.drawRect(0, y, stage.stageWidth * perc, BAR_HEIGHT);
        g.endFill();
    }

    protected var _loader :Loader;

    protected static const BAR_HEIGHT :int = 20;
}
}
