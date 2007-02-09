package {

import flash.display.Loader;
import flash.display.Sprite;

import flash.events.Event;

import flash.utils.ByteArray;


[SWF(width="161", height="150")]
public class ReWorker extends Sprite
{
    public function ReWorker ()
    {
        var l :Loader = new Loader();

        // register our pass-through adapter weakly
        l.contentLoaderInfo.sharedEvents.addEventListener("controlConnect",
            this.root.loaderInfo.sharedEvents.dispatchEvent, false, 0, true);

        // then, let's load the shiznat
        l.addEventListener(Event.COMPLETE, handleReady);
        l.loadBytes(new ORIG() as ByteArray);
    }

    protected function handleReady (evt :Event) :void
    {
        var l :Loader = (evt.currentTarget as EmbeddedSwfLoader);
        addChild(l.content);
    }

    [Embed(source="ImageFlipper.swf#ImageFlipper")]
    private static const ORIG :Class;
}
}
