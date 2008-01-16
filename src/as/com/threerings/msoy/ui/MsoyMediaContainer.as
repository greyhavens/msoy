//
// $Id$

package com.threerings.msoy.ui {

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;

import flash.display.Loader;
import flash.display.LoaderInfo;

import flash.utils.ByteArray;

import com.threerings.util.Log;
import com.threerings.util.StringUtil;
import com.threerings.util.Util;
import com.threerings.util.ValueEvent;

import com.threerings.flash.MediaContainer;
import com.threerings.flash.MenuUtil;

import com.whirled.DataPack;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.ContextMenuProvider;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Prefs;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.StaticMediaDesc;

public class MsoyMediaContainer extends MediaContainer
    implements ContextMenuProvider
{
    public function MsoyMediaContainer (desc :MediaDesc = null)
    {
        super(null);
        if (desc != null) {
            setMediaDesc(desc);
        }

        // have this container listen for bleep changes during its lifetime
        Prefs.config.addEventListener(Prefs.BLEEPED_MEDIA, handleBleepChange, false, 0, true);
    }

    override public function setMedia (url :String) :void
    {
        // TODO: ???
        // I think that if you're using this class you should be setting the media
        // via a MediaDesc, but don't want to completely break the behavior of super..
        log.warning("setMedia() called on a MsoyMediaContainer... letting it pass.");
        super.setMedia(url);
    }

    /**
     * Set a new MediaDescriptor.
     * @return true if it was accepted...
     */
    public function setMediaDesc (desc :MediaDesc) :void
    {
        if (Util.equals(desc, _desc)) {
            return;
        }

        _desc = desc;
        setIsBlocked(Prefs.isMediaBlocked(_desc.getMediaId()));
    }

    // TODO: doc
    public function isBlockable () :Boolean
    {
        return (_desc != null) && !(_desc is StaticMediaDesc);
    }

    // TODO: doc
    public function isBlocked () :Boolean
    {
        return Prefs.isMediaBlocked(_desc.getMediaId());
    }

    // TODO: doc
    public function toggleBlocked (ctx :MsoyContext = null) :void
    {
        var nowBlocked :Boolean = !isBlocked();
        // and change the setting. We'll get an event about the change, and react to that.
        Prefs.setMediaBlocked(_desc.getMediaId(), nowBlocked);

        // TEMP
        if (!_hasBleeped && ctx != null) {
            _hasBleeped = true;
            ctx.displayInfo(Msgs.GENERAL.getPath(), "m.bleeping_todo");
        }
    }

    // from ContextMenuProvider
    public function populateContextMenu (ctx :MsoyContext, menuItems :Array) :void
    {
        if (isBlockable()) {
            var isBlocked :Boolean = isBlocked();
            // TODO: if there happens to be another bleepable MsoyMediaContainer
            // also under the mouse, we'll probably clobber each other's menu items.
            // There's no human-meaningful identifier we can inject in the string from just
            // the MediaDesc. Punting!
            menuItems.push(MenuUtil.createControllerMenuItem(
                Msgs.GENERAL.get(isBlocked ? "b.unbleep_media" : "b.bleep_media"),
                toggleBlocked, ctx));
        }
    }

    // TODO: doc
    protected function setIsBlocked (blocked :Boolean) :void
    {
        var desc :MediaDesc;
        if (blocked) {
            desc = new StaticMediaDesc(MediaDesc.IMAGE_JPEG, Item.FURNITURE, "blocked");
        } else {
            desc = _desc;
        }
        super.setMedia(desc.getMediaPath());
    }

    /**
     * Called when a piece of media is bleeped or unbleeped.
     */
    protected function handleBleepChange (event :ValueEvent) :void
    {
        if (isBlockable()) {
            var id :String = _desc.getMediaId();
            if (id === event.value[0]) {
                setIsBlocked(Boolean(event.value[1]));
            }
        }
    }


    // ===== Everything from here on down is related to the media stub and to
    //       all-in-one (zip) media. This may not be needed here, and all this functionality
    //       could be moved to MsoySprite (or a new encapsulating subclass within MsoySprite)


    override protected function setupSwfOrImage (url :String) :void
    {
        var isZip :Boolean = isZipUrl(url);
        // if it's a zip, always start loading it in the background...
        if (isZip) {
            _pack = new DataPack(_url);
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

    /** Our Media descriptor. */
    protected var _desc :MediaDesc;

    /** The datapack. Used only while loading. */
    protected var _pack :DataPack;

    // TEMP: have we bleeped something (and issued the bleep disclaimer?)
    protected static var _hasBleeped :Boolean
}
}
