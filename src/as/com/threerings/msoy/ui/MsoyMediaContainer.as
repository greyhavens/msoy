//
// $Id$

package com.threerings.msoy.ui {

import flash.errors.IllegalOperationError;

import com.threerings.util.Util;
import com.threerings.util.ValueEvent;

import com.threerings.flash.MediaContainer;
import com.threerings.flash.MenuUtil;
import com.threerings.flash.video.SimpleVideoDisplay;

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

    override public function setMedia (url :String) :void
    {
        if (allowSetMedia()) {
            super.setMedia(url);

        } else {
            throw new IllegalOperationError("setMedia() should not be called " +
                "on a MsoyMediaContainer, use setMediaDesc instead.");
        }
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
        return (_desc != null) && !(_desc is StaticMediaDesc) && (_desc.getMediaPath() != null);
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

    override protected function showNewMedia (url :String) :void
    {
        // TODO: some stuff could be combined with VideoViewer, for sure
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
        switch (MediaDesc.suffixToMimeType(url)) {
        case MediaDesc.EXTERNAL_YOUTUBE:
            var player :YouTubePlayer = new YouTubePlayer();
            var vid :SimpleVideoDisplay = new SimpleVideoDisplay(player);
            _media = vid;
            addChildAt(vid, 0);
            updateContentDimensions(320, 240); // TODO?
            ExternalMediaUtil.fetch(url, player);
            break;

        default:
            super.setupVideo(url);
            break;
        }
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
        if (isBlockable()) {
            var id :String = _desc.getMediaId();
            if (id === event.value[0]) {
                setIsBlocked(Boolean(event.value[1]));
            }
        }
    }

    /**
     * Do we allow setMedia(url) to be called?
     */
    protected function allowSetMedia () :Boolean
    {
        return false;
    }

    /** Our Media descriptor. */
    protected var _desc :MediaDesc;

    // TEMP: have we bleeped something (and issued the bleep disclaimer?)
    protected static var _hasBleeped :Boolean
}
}
