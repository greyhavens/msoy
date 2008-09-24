package {

import flash.display.Loader;
import flash.display.Sprite;
import flash.display.StageAlign;
import flash.display.StageScaleMode;
import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;
import flash.net.URLLoader;
import flash.net.URLRequest;
import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.Security;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

// On Kongregate, the width limit is 700, but there appears to be
// no height limit.

// On newgrounds, width limit is 800, height limit 700

// miniclip- ? (new user registration was broken)

// addicting games: ?

// armorgames: dunno, submissions are reviewed, 1000 wide x 700 tall
// was submit-able


/**
 * RoomStub. A small wrapper to load up a whirled client and play a game
 * from some 3rd party site.
 *
 * NOTE: Please avoid using any non-builtin classes here.
 *  1) We shouldn't need to
 *  2) It helps keep the size down
 *  3) It avoids possible problems. For example, if we use StringUtil in here, then that's loaded
 *    in. We _do_ put the client in a new ApplicationDomain, but I'm still worried about
 *    possible collision if our client uses newly-added methods in StringUtil and this stub
 *    has defined a version of StringUtil that is old and lacking those methods.
 */
[SWF(width="700", height="575")]
public class RoomStub extends Sprite
{
    /** The id of the room we'd like to load. */
    public static const ROOM_ID :int = 5168;

    /** The server we're connecting with, with a trailing slash. */
    public static const CLIENT_URL :String = "http://www.whirled.com/clients/world-client.swf";
    //public static const SERVER :String = "http://tasman.sea.earth.threerings.net:8080/";

    public function RoomStub ()
    {
        if (stage != null) {
            stage.scaleMode = StageScaleMode.NO_SCALE;
            stage.align = StageAlign.TOP_LEFT;
        }

        _label = new TextField();
        _label.autoSize = TextFieldAutoSize.LEFT;
        _label.selectable = false;
        var tf :TextFormat = new TextFormat();
        tf.font = "_sans";
        tf.size = 18;
        tf.bold = true;
        tf.color = 0xFFFFFF;
        _label.defaultTextFormat = tf;
        addChild(_label);
        setLabel("Loading...");

        // allow all loaded content to cross-script this SWF
        // @TODO - is there any reason to make this more restrictive?
        Security.allowDomain("*");

        // now let's try loading the client
        _clientLoader = new Loader();
        _clientLoader.contentLoaderInfo.addEventListener(Event.INIT, onClientLoaded);
        _clientLoader.contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR, onClientLoadError);
        _clientLoader.load(new URLRequest(CLIENT_URL),
            new LoaderContext(true, new ApplicationDomain(null)));
        addChild(_clientLoader);
    }

    public function getWhirledParams () :String
    {
        // called by world-client
        return "sceneId=" + ROOM_ID;
    }

    protected function onClientDetailsError (e :ErrorEvent) :void
    {
        trace("client details load error: " + e);
        reportError(e);
    }

    protected function onClientLoaded (...ignored) :void
    {
        removeChild(_label);
        _label = null;
    }

    protected function onClientLoadError (e :ErrorEvent) :void
    {
        removeChild(_clientLoader);
        trace("client load error: " + e);
        reportError(e);
    }

    protected function reportError (e :ErrorEvent) :void
    {
        setLabel("Error loading: " + e.text);
    }

    protected function setLabel (s :String) :void
    {
        _label.text = s;
        _label.width = _label.textWidth + 5;
        _label.height = _label.textHeight + 4;
    }

    protected var _clientLoader :Loader;

    protected var _label :TextField;
}
}
