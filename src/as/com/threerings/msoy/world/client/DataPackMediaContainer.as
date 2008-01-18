//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;

import flash.display.Loader;
import flash.display.LoaderInfo;

import flash.utils.ByteArray;

import com.threerings.util.StringUtil;

import com.threerings.msoy.ui.MsoyMediaContainer;

import com.threerings.msoy.client.DeploymentConfig;

import com.threerings.msoy.world.data.MsoyDataPack;

/**
 * Extends MsoyMediaContainer to be able to deal with all-in-one media, that is, media
 * inside a zip file that is a datapack.
 *
 * Note that this is essentially an abstract class that should only be extended by MsoySprite.
 * I could roll all this into MsoySprite, but this is cleaner.
 */
public class DataPackMediaContainer extends MsoyMediaContainer
{
    public function DataPackMediaContainer ()
    {
        super(null);
    }

    /**
     * Get the default DataPack, with the _content removed, and clear it from memory.
     */
    public function getAndClearDataPack () :ByteArray
    {
        if (_pack == null) {
            return null;
        }

        var ba :ByteArray = _pack.toByteArray();
        _pack = null;
        return ba;
    }

    override protected function setupSwfOrImage (url :String) :void
    {
        var isZip :Boolean = isZipUrl(url);
        // if it's a zip, always start loading it in the background...
        if (isZip) {
            _pack = new MsoyDataPack(_url);
            _pack.addEventListener(Event.COMPLETE, handleZipComplete);
            _pack.addEventListener(ErrorEvent.ERROR, handleZipError);
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
            info.addEventListener(IOErrorEvent.IO_ERROR, handleStubIOError);
        }

        return loader;
    }

    /**
     * Should we use the stub to load the specified URL?
     */
    protected function shouldUseStub (url :String) :Boolean
    {
        // we use the stub only on non-file non-images
        return !(isImage(url) || isFileUrl(url));
    }

    /**
     * Handle the completion of the stub loading.
     */
    protected function handleStubComplete (event :Event) :void
    {
        var info :LoaderInfo = (event.target as LoaderInfo);

        if (isZipUrl(_url)) {
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
    protected function handleStubIOError (event :IOErrorEvent) :void
    {
        // we *may* be loading a datapack. If so, shut it down.
        if (_pack != null) {
            _pack.close();
            _pack = null;
        }

        // call the regular error handler
        handleIOError(event);
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
    protected function handleZipComplete (event :Event) :void
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
        if ((_pack == null) || !_pack.isComplete()) {
            return;
        }

        var usingStub :Boolean = shouldUseStub(_url);

        // make sure the stub is ready
        if (usingStub && (Loader(_media).content == null)) {
            return;
        }

        // if both are ready, make it happen
        var ba :ByteArray = _pack.getFile("_content");
        if (ba == null) {
            handleZipError(null);
            return;
        }

        // great! remove that file
        _pack.removeFile("_content");

        if (!usingStub) {
            Loader(_media).loadBytes(ba);
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
    protected function handleZipError (event :ErrorEvent) :void
    {
        _pack = null;
        stoppedLoading();
        setupBrokenImage(-1, -1);
    }

    /** The datapack. Used only while loading. */
    protected var _pack :MsoyDataPack;
}
}
