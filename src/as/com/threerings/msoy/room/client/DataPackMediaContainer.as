//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;

import flash.display.Loader;
import flash.display.LoaderInfo;

import flash.net.URLLoader;
import flash.net.URLLoaderDataFormat;
import flash.net.URLRequest;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.Security;

import flash.utils.ByteArray;

import com.threerings.util.StringUtil;

import com.threerings.msoy.ui.MsoyMediaContainer;

import com.threerings.msoy.client.DeploymentConfig;

import com.threerings.msoy.room.data.MsoyDataPack;

/**
 * Extends MsoyMediaContainer to be able to deal with all-in-one media, that is, media
 * inside a zip file that is a datapack.
 *
 * Note that this is essentially an abstract class that should only be extended by MsoySprite.
 * I could roll all this into MsoySprite, but this is cleaner.
 */
//
// NOTE:
// If a bug in FZip is fixed that allows re-zipping, we can roll back to r7363
// and only provide the bytes needed by the _CONTENT (and not _CONTENT itself).
//
public class DataPackMediaContainer extends MsoyMediaContainer
{
    public function DataPackMediaContainer ()
    {
        super(null);
    }

    /**
     * Get the default DataPack as bytes, and clear it from memory.
     */
    public function getAndClearDataPack () :ByteArray
    {
        if (_packLoader == null) {
            return null;
        }

        var ba :ByteArray = _packLoader.data as ByteArray;
        _packLoader = null;
        return ba;
    }

    /**
     * Set the media to be displayed as a ByteArray representing zipped (remixable) media.
     */
    public function setZippedMediaBytes (zippedBytes :ByteArray) :void
    {
        if (_media != null) {
            shutdown(false);
        }
        _url = null;

        // this is funny, but it works
        _packLoader = new URLLoader();
        _packLoader.data = zippedBytes;

        // load it!
        willShowNewMedia();
        startedLoading();
        setupSwfOrImage(_url);
        if (!shouldUseStub(_url)) {
            initLoader();
            checkPackComplete(); // in here the bytes will be extracted
            didShowNewMedia();
        }
    }

    override protected function setupSwfOrImage (url :String) :void
    {
        var isZip :Boolean = url != null && isZipUrl(url);
        // if it's a zip, always start loading it in the background...
        if (isZip) {
            _packLoader = new URLLoader();
            _packLoader.dataFormat = URLLoaderDataFormat.BINARY;
            _packLoader.addEventListener(Event.COMPLETE, handlePackComplete);
            _packLoader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handlePackError);
            _packLoader.addEventListener(IOErrorEvent.IO_ERROR, handlePackError);
            _packLoader.load(new URLRequest(_url));
        }

        if (shouldUseStub(url)) {
            // load the stub instead
            // TODO: get this URL from elsewhere?
            url = DeploymentConfig.mediaURL + "MediaStub.swf";

        } else if (isZip) {
            // we must be loading a zip off the filesystem!
            startedLoading();
            // set up the loader, we'll fill it in in handleZipComplete
            initLoader();
            return; // EXIT- do not call super
        }

        super.setupSwfOrImage(url);
    }

    override protected function initLoader () :Loader
    {
        var loader :Loader = super.initLoader();

        if (shouldUseStub(_url)) {
            // if loading the stub, we only care about COMPLETE and IO_ERROR
            var info :LoaderInfo = loader.contentLoaderInfo;

            removeListeners(info);
            info.addEventListener(Event.COMPLETE, handleStubComplete);
            info.addEventListener(IOErrorEvent.IO_ERROR, handleStubError);
            info.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleStubError);
        }

        return loader;
    }

    /**
     * Should we use the stub to load the specified URL?
     */
    protected function shouldUseStub (url :String) :Boolean
    {
        if (url == null) {
            return (Security.sandboxType != Security.LOCAL_WITH_FILE);
        }

        // we use the stub only on non-file non-images
        return !(isImage(url) || isFileUrl(url));
    }

    /**
     * Handle the completion of the stub loading.
     */
    protected function handleStubComplete (event :Event) :void
    {
        var info :LoaderInfo = (event.target as LoaderInfo);

        if (_url == null || isZipUrl(_url)) {
            checkPackComplete();
            return;
        }

        try {
            // have the stub load the REAL url
            info = Object(info.loader.content).load(_url) as LoaderInfo;
            if (info != null) {
                addListeners(info);
                return; // EXIT
            }

        } catch (err :Error) {
            log.logStackTrace(err);
        }

        // otherwise, if we didn't exit above,
        trace("Whaddafuck: " + _url);
        handleComplete(event);
    }

    /**
     * Handle an error while loading the stub.
     */
    protected function handleStubError (event :ErrorEvent) :void
    {
        // we *may* be loading a datapack. If so, shut it down.
        if (_packLoader != null) {
            try {
                _packLoader.close();
            } catch (err :Error) {
                // no worries
            }
            _packLoader = null;
        }

        // call the regular error handler
        handleError(event);
    }

    /**
     * Is the specified URL to a zip file?
     */
    protected function isZipUrl (url :String) :Boolean
    {
        return StringUtil.endsWith(url.toLowerCase(), ".zip");
    }

    /**
     * Is the specified URL a "file" url?
     */
    protected function isFileUrl (url :String) :Boolean
    {
        return StringUtil.startsWith(url.toLowerCase(), "file:");
    }

    /**
     * Handle the COMPLETE event while loading the zip file.
     */
    protected function handlePackComplete (event :Event) :void
    {
        checkPackComplete();
    }

    /**
     * Check to see if both the stub and the datapack are loaded and start
     * loading the content bytes.
     */
    protected function checkPackComplete () :void
    {
        // make sure the pack is ready. If the pack is null it means we already
        // had an error loading the zip and we should just exit uneventfully here.
        if ((_packLoader == null) || (_packLoader.data == null)) {
            return;
        }

        var usingStub :Boolean = shouldUseStub(_url);

        // make sure the stub is ready
        if (usingStub && (Loader(_media).content == null)) {
            return;
        }

        var pack :MsoyDataPack = new MsoyDataPack(_packLoader.data);

        // if both are ready, make it happen
        var ba :ByteArray = pack.getContent();
        if (ba == null) {
            handlePackError("No content found in DataPack.");
            return;
        }

        if (!usingStub) {
            Loader(_media).loadBytes(ba, new LoaderContext(false, new ApplicationDomain(null)));
            // all the regular listeners are already installed on this listener

        } else {
            try {
                // have the stub load these bytes
                var info :LoaderInfo = Object(_media).content.loadBytes(ba) as LoaderInfo;
                if (info != null) {
                    addListeners(info);
                    return; // EXIT
                }

            } catch (err :Error) {
                log.logStackTrace(err);
            }

            // otherwise, if we didn't exit above,
            trace("Wigglefuck: " + _url);
        }
    }

    /**
     * Handle an error while loading a zip file.
     */
    protected function handlePackError (eventOrErrorMsg :Object) :void
    {
        log.warning("Error loading media datapack [error=" + eventOrErrorMsg + "].");
        _packLoader = null;
        stoppedLoading();
        setupBrokenImage(-1, -1);
    }

    /** The URLLoader used to load the bytes for a DataPack. */
    protected var _packLoader :URLLoader;
}
}
