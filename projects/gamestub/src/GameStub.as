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

public class GameStub extends Sprite
{
    public function GameStub ()
    {
        if (stage != null) {
            stage.scaleMode = StageScaleMode.NO_SCALE;
            stage.align = StageAlign.TOP_LEFT;
        }

        // ask Whirled where to find the SWF client that we'll load to play our game
        _clientDetailsLoader = new URLLoader();
        _clientDetailsLoader.addEventListener(Event.COMPLETE, onClientDetailsLoaded);
        _clientDetailsLoader.addEventListener(IOErrorEvent.IO_ERROR, onClientDetailsError);
        _clientDetailsLoader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, onClientDetailsError);

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

        var clientUrl :String = String(details.url[0]);
        trace("GameStub: loading '" + clientUrl + "'");

        _whirledParams = details.whirledParams[0];

        // allow all loaded content to cross-script this SWF
        // @TODO - is there any reason to make this more restrictive?
        Security.allowDomain("*");

        // now let's try loading the client
        _clientLoader = new Loader();
        _clientLoader.contentLoaderInfo.addEventListener(Event.INIT, onClientLoaded);
        _clientLoader.contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR, onClientLoadError);
        _clientLoader.load(new URLRequest(clientUrl), new LoaderContext(true, new ApplicationDomain(null)));
    }

    protected function onClientDetailsError (e :ErrorEvent) :void
    {
        trace("client details load error: " + e);
    }

    protected function onClientLoaded (...ignored) :void
    {
        trace("client loaded");
        this.addChild(_clientLoader);
    }

    protected function onClientLoadError (e :ErrorEvent) :void
    {
        trace("client load error: " + e);
    }

    protected var _clientDetailsLoader :URLLoader;
    protected var _clientLoader :Loader;
    protected var _whirledParams :String;

    protected static const GAME_ID :uint = 10;
    protected static const CLIENT_VERSION :uint = 0;

    protected static const URL :String =
        "http://noevil.sea.earth.threerings.net:8080/gamestubsvc?gameId=" + GAME_ID + "&v=" + CLIENT_VERSION;
}

}
