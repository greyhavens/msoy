//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObject;
import flash.display.LoaderInfo;

import flash.events.Event;
import flash.events.IEventDispatcher;

import mx.core.Application;
import mx.core.ISWFBridgeProvider;

import mx.events.SWFBridgeEvent;

import com.threerings.util.NamedValueEvent;

import com.threerings.ui.MenuUtil;
import com.threerings.media.VideoPlayer;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.ui.MediaDescContainer;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.ContextMenuProvider;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.utils.Capabilities;

import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.item.client.ExternalMediaUtil;
import com.threerings.msoy.item.client.YouTubePlayer;
import com.threerings.msoy.item.data.all.DefaultItemMediaDesc;
import com.threerings.msoy.item.data.all.Item;

public class MsoyMediaContainer extends MediaDescContainer
    implements ContextMenuProvider, ISWFBridgeProvider
{
    public function MsoyMediaContainer (desc:MediaDesc = null, bleepInMenu :Boolean = true,
        suppressHitTestPoint :Boolean = false, blockType :String = null)
    {
        super(desc);
        _bleepInMenu = bleepInMenu;
        _blockType = blockType;
        _suppressHitTestPoint = suppressHitTestPoint;

        // have this container listen for bleep changes during its lifetime
        Prefs.events.addEventListener(Prefs.BLEEPED_MEDIA, handleBleepChange, false, 0, true);
    }

    override public function setMediaDesc (desc :MediaDesc) :Boolean
    {
        if (super.setMediaDesc(desc)) {
            checkBlocked();
            return true;
        }
        return false;
    }

    /**
     * Tests if this media may be blocked (aka bleeped) by the local user.
     */
    public function isBleepable () :Boolean
    {
        return (_desc != null) && _desc.isBleepable();
    }

    /**
     * Toggle the bleeped status of the media we're holding.
     */
    public function toggleBleeped (ctx :MsoyContext = null) :void
    {
        var nowBleeped :Boolean = !isBleeped();
        // and change the setting. We'll get an event about the change, and react to that.
        Prefs.setMediaBleeped(_desc.getMediaId(), nowBleeped);
    }

    /**
     * Is the media contained herein specifically bleeped?
     * This does not check the global bleep.
     */
    public function isBleeped () :Boolean
    {
        return isBleepable() && Prefs.isMediaBleeped(_desc.getMediaId());
    }

    /**
     * Set the block type of this media container, which will replace our actual configured
     * media with something fixed (typically static and low-bandwidth). A null argument will
     * clear the block. The block type takes precedence over bleeping, which is another form
     * of blocking.
     */
    public function setBlocked (blockType :String) :void
    {
        _blockType = blockType;
        checkBlocked();
    }

    /**
     * Tests if this media is blocked because of bleeping OR ANY OTHER REASON for the local user.
     * This can be due to either specific blocking of just this media, the global blocking of
     * all media, or anything else.
     */
    public function isBlocked () :Boolean
    {
        return (null != getBlockType());
    }

    /**
     * Re-check the blocked status of this media.
     */
    public function checkBlocked () :void
    {
        setIsBlocked(getBlockType());
    }

    public function setSuppressHitTestPoint (suppress :Boolean) :void
    {
        _suppressHitTestPoint = suppress;
    }

    public function setMaxContentDimensions (width :int, height :int) :void
    {
        _maxWidth = width;
        _maxHeight = height;
    }

    // documentation inherited
    override public function hitTestPoint (
        x :Number, y :Number, shapeFlag :Boolean = false) :Boolean
    {
        if (_suppressHitTestPoint) {
            return false;
        }
        return super.hitTestPoint(x, y, shapeFlag);
    }

    /** @inheritDoc */
    // from MediaContainer
    override public function getMediaScaleX () :Number
    {
        // use a fixed scale for blocked media
        return isBlocked() ? 1 : _spriteMediaScaleX;
    }

    /** @inheritDoc */
    // from MediaContainer
    override public function getMediaScaleY () :Number
    {
        // use a fixed scale for blocked media
        return isBlocked() ? 1 : _spriteMediaScaleY;
    }

    /**
     * Set the media scale to use when we are not displaying a blocked state.
     */
    public function setSpriteMediaScale (scaleX :Number, scaleY :Number) :void
    {
        _spriteMediaScaleX = scaleX;
        _spriteMediaScaleY = scaleY;
    }

    override public function getMaxContentWidth () :int
    {
        return _maxWidth;
    }

    override public function getMaxContentHeight () :int
    {
        return _maxHeight;
    }

    public function getUnscaledWidth () :Number
    {
        return _w;
    }

    public function getUnscaledHeight () :Number
    {
        return _h;
    }

    // from ContextMenuProvider
    public function populateContextMenu (ctx :MsoyContext, menuItems :Array) :void
    {
        if (_bleepInMenu && isBleepable()) {
            var isBleeped :Boolean = isBleeped();
            // TODO: if there happens to be another bleepable MsoyMediaContainer
            // also under the mouse, we'll probably clobber each other's menu items.
            // There's no human-meaningful identifier we can inject in the string from just
            // the MediaDesc. Punting!
            menuItems.push(MenuUtil.createCommandContextMenuItem(
                Msgs.GENERAL.get(isBleeped ? "b.unbleep_media" : "b.bleep_media"),
                toggleBleeped, ctx));
        }
    }

    // from ISWFBridgeProvider
    public function get swfBridge () :IEventDispatcher
    {
        return _bridge;
    }

    // from ISWFBridgeProvider
    public function get childAllowsParent () :Boolean
    {
        return true;
    }

    // from ISWFBridgeProvider
    public function get parentAllowsChild () :Boolean
    {
        return false;
    }

    override protected function addListeners (info :LoaderInfo) :void
    {
        super.addListeners(info);

        if (Capabilities.isFlash10() && "uncaughtErrorEvents" in Object(info.loader)) {
            Object(info.loader).uncaughtErrorEvents.addEventListener(
                "uncaughtError", handleUncaughtErrors);
        }
        info.sharedEvents.addEventListener(SWFBridgeEvent.BRIDGE_NEW_APPLICATION, bridgeApp);
    }

    override protected function removeListeners (info :LoaderInfo) :void
    {
        super.removeListeners(info);

        if (Capabilities.isFlash10() && "uncaughtErrorEvents" in Object(info.loader)) {
            Object(info.loader).uncaughtErrorEvents.removeEventListener(
                "uncaughtError", handleUncaughtErrors);
        }
        info.sharedEvents.removeEventListener(SWFBridgeEvent.BRIDGE_NEW_APPLICATION, bridgeApp);
    }

    protected function handleUncaughtErrors (event :*) :void
    {
        log.info("Uncaught Error", "media", _desc, event);
    }

    protected function bridgeApp (event :Event) :void
    {
        _bridge = IEventDispatcher(event.currentTarget);
        Application(Application.application).systemManager.addChildBridge(_bridge, this);
    }

    override protected function showNewMedia (url :String) :void
    {
        switch (MediaMimeTypes.suffixToMimeType(url)) {
        case MediaMimeTypes.VIDEO_FLASH:
        case MediaMimeTypes.EXTERNAL_YOUTUBE:
            setupVideo(url);
            break;

        default:
            super.showNewMedia(url);
            break;
        }
    }

    override protected function setupVideo (url :String) :void
    {
        if (MediaMimeTypes.suffixToMimeType(url) == MediaMimeTypes.EXTERNAL_YOUTUBE) {
            var ytPlayer :YouTubePlayer = new YouTubePlayer();
            _media = createVideoUI(ytPlayer);
            addChildAt(_media, 0);
            updateContentDimensions(_media.width, _media.height);
            ExternalMediaUtil.fetch(url, ytPlayer);

        } else {
            super.setupVideo(url);
        }
    }

    override protected function createVideoUI (player :VideoPlayer) :DisplayObject
    {
        return new MsoyVideoDisplay(player);
    }

    /**
     * Get the type of block being done on this media.
     * This string can be used for translation, potentially, or for turning into
     * a static media desc. See setIsBlocked().
     */
    protected function getBlockType () :String
    {
        if (_blockType != null) {
            return _blockType;
        }
        return (Prefs.isGlobalBleep() || isBleeped()) ? "bleep" : null;
    }

    /**
     * Set the blockType being used on this media, or null if not blocked.
     */
    protected function setIsBlocked (blockType :String) :void
    {
        var desc :MediaDesc;
        if (blockType != null) {
            desc = new DefaultItemMediaDesc(MediaMimeTypes.IMAGE_PNG, Item.FURNITURE, blockType);
        } else {
            desc = _desc;
        }
        super.setMedia((desc == null) ? null : desc.getMediaPath());
    }

    /**
     * Called when a piece of media is bleeped or unbleeped.
     */
    protected function handleBleepChange (event :NamedValueEvent) :void
    {
        if (isBleepable() &&
                (event.name == Prefs.GLOBAL_BLEEP || event.name == _desc.getMediaId())) {
            checkBlocked();
        }
    }

    override protected function shutdownMedia () :void
    {
        if (_media is MsoyVideoDisplay) {
            MsoyVideoDisplay(_media).unload();

        } else {
            super.shutdownMedia();
        }
    }

    protected var _suppressHitTestPoint :Boolean;

    protected var _maxWidth :int = int.MAX_VALUE;
    protected var _maxHeight :int = int.MAX_VALUE;

    /** The media scale to use when we are not blocked. */
    protected var _spriteMediaScaleX :Number = 1.0;

    /** The media scale to use when we are not blocked. */
    protected var _spriteMediaScaleY :Number = 1.0;

    /** Whether or not we should populate the context menu with a bleep action. */
    protected var _bleepInMenu :Boolean;

    /** A settable block type that will, if non-null, override bleep considerations. */
    protected var _blockType :String;

    protected var _bridge :IEventDispatcher;
}
}
