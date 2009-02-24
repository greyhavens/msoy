//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObject;

import com.threerings.util.Util;
import com.threerings.util.ValueEvent;

import com.threerings.flash.MediaContainer;
import com.threerings.flash.MenuUtil;
import com.threerings.flash.media.VideoPlayer;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.ContextMenuProvider;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Prefs;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.StaticMediaDesc;
import com.threerings.msoy.item.client.ExternalMediaUtil;
import com.threerings.msoy.item.client.YouTubePlayer;
import com.threerings.msoy.item.data.all.DefaultItemMediaDesc;
import com.threerings.msoy.item.data.all.Item;

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

    /**
     * ATTENTION: don't use this method in msoy unless you know what you're doing.
     * MediaDescs should almost always be used in msoy instead of urls.
     */
    override public function setMedia (url :String) :void
    {
        // this method exists purely for the change in documentation.
        super.setMedia(url);
    }

    /**
     * Set a new MediaDescriptor.
     */
    public function setMediaDesc (desc :MediaDesc) :void
    {
        if (Util.equals(desc, _desc)) {
            return;
        }

        _desc = desc;
        checkBlocked();
    }

    /**
     * Tests if this media may be blocked (aka bleeped) by the local user.
     */
    public function isBleepable () :Boolean
    {
        return (_desc != null) && !(_desc is StaticMediaDesc) && (_desc.getMediaPath() != null);
    }

    /**
     * Toggle the bleeped status of the media we're holding.
     */
    public function toggleBleeped (ctx :MsoyContext = null) :void
    {
        var nowBleeped :Boolean = !isBleeped();
        // and change the setting. We'll get an event about the change, and react to that.
        Prefs.setMediaBleeped(_desc.getMediaId(), nowBleeped);

        // TEMP
        if (!_hasBleeped && ctx != null) {
            ctx.displayInfo(Msgs.GENERAL.getPath(), "m.bleeping_todo");
            _hasBleeped = true;
        }
    }

    /**
     * Is the media contained herein specifically bleeped?
     */
    public function isBleeped () :Boolean
    {
        return isBleepable() && Prefs.isMediaBleeped(_desc.getMediaId());
    }

    /**
     * Tests if this media is blocked because of bleeping OR ANY OTHER REASON for the local user.
     * This can be due to either specific blocking of just this media, the global blocking of
     * all media, or anything else.
     */
    public function isBlocked () :Boolean
    {
        return Prefs.isGlobalBleep() || isBleeped();
    }

    /**
     * Re-check the blocked status of this media.
     */
    public function checkBlocked () :void
    {
        setIsBlocked(isBlocked());
    }

    // from ContextMenuProvider
    public function populateContextMenu (ctx :MsoyContext, menuItems :Array) :void
    {
        if (isBleepable()) {
            var isBleeped :Boolean = isBleeped();
            // TODO: if there happens to be another bleepable MsoyMediaContainer
            // also under the mouse, we'll probably clobber each other's menu items.
            // There's no human-meaningful identifier we can inject in the string from just
            // the MediaDesc. Punting!
            menuItems.push(MenuUtil.createControllerMenuItem(
                Msgs.GENERAL.get(isBleeped ? "b.unbleep_media" : "b.bleep_media"),
                toggleBleeped, ctx));
        }
    }

    override protected function showNewMedia (url :String) :void
    {
        switch (MediaDesc.suffixToMimeType(url)) {
        case MediaDesc.VIDEO_FLASH:
        case MediaDesc.EXTERNAL_YOUTUBE:
            setupVideo(url);
            break;

        default:
            super.showNewMedia(url);
            break;
        }
    }

    override protected function setupVideo (url :String) :void
    {
        if (MediaDesc.suffixToMimeType(url) == MediaDesc.EXTERNAL_YOUTUBE) {
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

    // TODO: doc
    protected function setIsBlocked (blocked :Boolean) :void
    {
        var desc :MediaDesc;
        if (blocked) {
            desc = new DefaultItemMediaDesc(MediaDesc.IMAGE_JPEG, Item.FURNITURE, "blocked");
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
        if (isBleepable()) {
            const id :String = String(event.value[0]);
            if (id == Prefs.GLOBAL_BLEEP || id == _desc.getMediaId()) {
                checkBlocked();
            }
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

    /** Our Media descriptor. */
    protected var _desc :MediaDesc;

    // TEMP: have we bleeped something (and issued the bleep disclaimer?)
    protected static var _hasBleeped :Boolean
}
}
