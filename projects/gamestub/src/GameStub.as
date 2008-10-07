package {

import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Sprite;
import flash.display.StageAlign;
import flash.display.StageScaleMode;
import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;
import flash.external.ExternalInterface;
import flash.net.LocalConnection;
import flash.net.URLLoader;
import flash.net.URLRequest;
import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.Security;
import flash.text.TextField;
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
    public static const WIDTH :int = 700;
    public static const HEIGHT :int = 575;

    /** The id of the game we'd like to load. */
    public static const GAME_ID :int = 8;

    //public static const CLIENT_URL :String = "http://www.whirled.com/clients/world-client.swf";
    public static const CLIENT_URL :String = "http://tasman.sea.earth.threerings.net:8080/clients/world-client.swf";

    public function GameStub ()
    {
        if (stage != null) {
            stage.scaleMode = StageScaleMode.NO_SCALE;
            stage.align = StageAlign.TOP_LEFT;
        }

        _label = new TextField();
        _label.width = WIDTH;
        _label.height = HEIGHT;
        _label.selectable = false;
        _label.wordWrap = true;
        var tf :TextFormat = new TextFormat();
        tf.font = "_sans";
        tf.size = 18;
        tf.bold = true;
        tf.color = 0xFFFFFF;
        _label.defaultTextFormat = tf;
        addChild(_label);
        _label.text = "Loading...";

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
        return "gameLobby=" + GAME_ID +
            "&vec=e." + encodeURIComponent(getHost()) + ".games." + GAME_ID;
    }

    protected function onClientLoaded (...ignored) :void
    {
        removeChild(_label);
        _label = null;
    }

    protected function onClientLoadError (e :ErrorEvent) :void
    {
        removeChild(_clientLoader);
        _label.text = "Error loading: " + e.text;
    }

    protected function getHost () :String
    {
        trace("==== The url: " + this.loaderInfo.url);
        trace("==== Just the domain ma'am: " + new LocalConnection().domain);
        try {
            trace("== with feeling: " + ExternalInterface.call("window.location.href.toString"));
        } catch (e :Error) {
            // le boo, le hoo
        }

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
        return massageHost(host);
    }

    protected function massageHost (host :String) :String
    {
        switch (host) {
        default: return host;
        case "ungrounded": return "newgrounds";
        }
    }

    protected static const URL_REGEXP :RegExp = /^(\w+:\/\/)?\/?([^:\/\s]+)/; // protocol and host

    protected var _clientLoader :Loader;

    protected var _label :TextField;
}
}
