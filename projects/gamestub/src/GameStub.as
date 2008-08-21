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

import com.threerings.flash.TextFieldUtil;

// On Kongregate, the width limit is 700, but there appears to be
// no height limit.

// On newgrounds, width limit is 800, height limit 700

// miniclip- ? (new user registration was broken)

// addicting games: ?

// armorgames: dunno, submissions are reviewed, 1000 wide x 700 tall
// was submit-able

[SWF(width="700", height="575")]
public class GameStub extends Sprite
{
    /** The id of the game we'd like to load. */
    public static const GAME_ID :int = 12;

    /** The affiliate we're building this for. */
    public static const AFFILIATE :String = "kongregate";

    public function GameStub ()
    {
        if (stage != null) {
            stage.scaleMode = StageScaleMode.NO_SCALE;
            stage.align = StageAlign.TOP_LEFT;
        }

        _label = TextFieldUtil.createField("Loading...",
            {
                autoSize: TextFieldAutoSize.LEFT, selectable: false
            }, {
                size: 18, bold: true, color: 0xFFFFFF
            });
        addChild(_label);

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
            TextFieldUtil.updateText(_label, String(error));
            return;
        }

        var clientUrl :String = String(details.url[0]);
        trace("GameStub: loading '" + clientUrl + "'");

        _whirledParams = details.params[0];

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
        TextFieldUtil.updateText(_label, "Error loading: " + e.text);
    }

    protected var _clientDetailsLoader :URLLoader;
    protected var _clientLoader :Loader;
    protected var _whirledParams :String;

    protected var _label :TextField;

    protected static const STUB_VERSION :uint = 0;

    protected static const URL :String =
        "http://tasman.sea.earth.threerings.net:8080/gamestubsvc" +
        "?gameId=" + GAME_ID +
        "&aff=" + AFFILIATE +
        "&v=" + STUB_VERSION;
}
}
