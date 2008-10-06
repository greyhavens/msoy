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
 * GameStub. A small wrapper to load up a whirled client and play a game
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
public class GameStub extends Sprite
{
    /** The id of the game we'd like to load. */
    public static const GAME_ID :int = 8;

    /** The server we're connecting with, with a trailing slash. */
    public static const SERVER :String = "http://www.whirled.com/";
    //public static const SERVER :String = "http://tasman.sea.earth.threerings.net:8080/";

    public function GameStub ()
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

        // ask Whirled where to find the SWF client that we'll load to play our game
        _clientDetailsLoader = new URLLoader();
        _clientDetailsLoader.addEventListener(Event.COMPLETE, onClientDetailsLoaded);
        _clientDetailsLoader.addEventListener(IOErrorEvent.IO_ERROR, onClientDetailsError);
        _clientDetailsLoader.addEventListener(SecurityErrorEvent.SECURITY_ERROR,
            onClientDetailsError);

        _clientDetailsLoader.load(new URLRequest(URL));
    }

    public function getWhirledParams () :String
    {
        // called by world-client
        return _whirledParams;
    }

    protected function onClientDetailsLoaded (...ignored) :void
    {
        // Client details are returned as XML.
        var details :XML = XML(_clientDetailsLoader.data);

        var error :Object = details.error[0];
        if (error != null) {
            setLabel(String(error));
            return;
        }

        var clientUrl :String = String(details.url[0]);
        trace("GameStub: loading '" + clientUrl + "'");

        _whirledParams = details.params[0] + "&" + getVectorParam();

        // allow all loaded content to cross-script this SWF
        // @TODO - is there any reason to make this more restrictive?
        Security.allowDomain("*");

        // now let's try loading the client
        _clientLoader = new Loader();
        _clientLoader.contentLoaderInfo.addEventListener(Event.INIT, onClientLoaded);
        _clientLoader.contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR, onClientLoadError);
        _clientLoader.load(new URLRequest(clientUrl),
            new LoaderContext(true, new ApplicationDomain(null)));
        addChild(_clientLoader);
    }

    protected function getVectorParam () :String
    {
        return "vec=e." + encodeURIComponent(massageHost(getHost())) + ".games." + GAME_ID;
    }

    protected function massageHost (host :String) :String
    {
        switch (host) {
        default: return host;
        case "ungrounded": return "newgrounds";
        }
    }

    protected function getHost () :String
    {
        var result :Object = URL_REGEXP.exec(this.loaderInfo.url);
        if (result == null) {
            return "";
        }

        var host :String = String(result[2]);
        // strip the last part
        var lastdot :int = host.lastIndexOf(".");
        if (lastdot != -1) {
            host = host.substring(0, lastdot);
        }
        // now just keep the last part
        lastdot = host.lastIndexOf(".");
        if (lastdot != -1) {
            host = host.substring(lastdot + 1);
        }
        return host;
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

    protected var _clientDetailsLoader :URLLoader;
    protected var _clientLoader :Loader;
    protected var _whirledParams :String;

    protected var _label :TextField;

    protected static const URL_REGEXP :RegExp = /^(\w+:\/\/)?\/?([^:\/\s]+)/; // protocol and host..

    protected static const STUB_VERSION :uint = 1;

    protected static const URL :String = SERVER + "gamestubsvc" + 
        "?gameId=" + GAME_ID +
        "&v=" + STUB_VERSION;
}
}
