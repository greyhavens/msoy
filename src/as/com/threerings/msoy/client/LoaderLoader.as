//
// $Id: LoaderLoader.as 17792 2009-08-11 01:47:37Z ray $

package com.threerings.msoy.client {

import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Sprite;
import flash.display.StageAlign;
import flash.display.StageScaleMode;
import flash.events.Event;
import flash.net.URLLoader;
import flash.net.URLRequest;
import flash.net.URLVariables;
import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.SecurityDomain;
import flash.utils.setTimeout;

import com.threerings.display.LoaderUtil;

/**
 * Loads the "Loader" and loads the main world client.
 */
[SWF(width="700", height="500")]
public class LoaderLoader extends Sprite
{
    public function LoaderLoader ()
    {
        super();
        root.loaderInfo.addEventListener(Event.INIT, initialize);
    }

    protected function initialize (... ignored) :void
    {
        prepStage();
        findLoader(root.loaderInfo.parameters);
    }

    /**
     * Prepare the stage.
     */
    protected function prepStage () :void
    {
        if (stage == null) {
            return;
        }

        stage.scaleMode = StageScaleMode.NO_SCALE;
        stage.align = StageAlign.TOP_LEFT;
        // TODO: any other cleanup to reset the stage after we remove the loader
    }

    protected function findLoader (params :Object) :void
    {
        var vars :URLVariables = new URLVariables();
        for (var s :String in params) {
            vars[s] = params[s];
        }
        trace("Now loading. Vars is " + vars);
        var req :URLRequest = new URLRequest(LOADER_ID_URL);
        req.data = vars;
        _loaderIdentifier = new URLLoader();
        _loaderIdentifier.addEventListener(Event.COMPLETE, handleIdComplete);
        _loaderIdentifier.load(req);
    }

    protected function handleIdComplete (event :Event) :void
    {
        var url :String = String(_loaderIdentifier.data);
        // TEMP TODO
//        if (url == "ooo") {
//            loadClient();
//            return;
//        }
        if (url == "ooo") {
            url =
                //"http://192.168.54.53:8080/media/" +
                "http://malacca.sea.earth.threerings.net:8080/media/" +
                "78ba0bf64534ecab9b13af0896f0b921c60a6786.swf";
        }
        closeLoaderIdentifier();
        loadLoader(url);
    }

    protected function loadLoader (loaderURL :String) :void
    {
        _loaderLoader = new Loader();
        addChild(_loaderLoader); // we do add this fucker to the stage
        _loaderLoader.contentLoaderInfo.addEventListener(Event.COMPLETE, handleLoaderComplete);
        _loaderLoader.load(new URLRequest(loaderURL),
            //null);
            //new LoaderContext(false, new ApplicationDomain(null), null));
            new LoaderContext(false, new ApplicationDomain(null), SecurityDomain.currentDomain));
    }

    protected function handleLoaderComplete (event :Event) :void
    {
        // TODO... for now we load the client here
        //loadClient();
        setTimeout(loadClient, 2500);
    }

    /**
     * Load the whirled client, in the background, only slapping it on the stage when it's ready.
     */
    protected function loadClient () :void
    {
        _clientLoader = new Loader();
        _clientLoader.contentLoaderInfo.addEventListener(Event.COMPLETE, handleClientComplete);
        _clientLoader.load(new URLRequest(CLIENT_URL));
        // we do not add the fucker to the stage.
    }

    /**
     * Handle that the main client has finished loading.
     */
    protected function handleClientComplete (event :Event) :void
    {
        closeLoaderIdentifier();
        closeLoader();
        prepStage();

        // let's put the thing on the stage
        stage.addChild(_clientLoader.content);

        // fuck my twinkies, why is this part necessary? Shouldn't flash like um fucking handle it?
        // TODO: look into
        stage.addEventListener(Event.RESIZE, handleStageResize);
        handleStageResize(event); // resize it now.
    }

    protected function closeLoaderIdentifier () :void
    {
        if (_loaderIdentifier != null) {
            try {
                _loaderIdentifier.close();
            } catch (e :Error) {
                trace("Got e: " + e);
            }
            _loaderIdentifier = null;
        }
    }

    protected function closeLoader () :void
    {
        if (_loaderLoader != null) {
            removeChild(_loaderLoader);
            trace("Unloading the loader!!!");
            LoaderUtil.unload(_loaderLoader);
            _loaderLoader = null;
        }
    }

    protected function handleStageResize (event :Event) :void
    {
        _clientLoader.content["setActualSize"](stage.stageWidth, stage.stageHeight);
    }

    protected var _loaderIdentifier :URLLoader;

    /** Loads the "loader", which is shown while the client loads. */
    protected var _loaderLoader :Loader;

    /** Loads the client. */
    protected var _clientLoader :Loader;

    protected static const LOADER_ID_URL :String =
        "http://malacca.sea.earth.threerings.net:8080/" + // DeploymentConfig ?
        "loadersvc";

    /** The url of the full whirled client. */
    protected static const CLIENT_URL :String =
        "http://malacca.sea.earth.threerings.net:8080/clients/0/" + // DeploymentConfig ?
        "main-client.swf";
}
}
