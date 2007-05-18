//
// $Id$

package com.threerings.msoy.ui {

import com.threerings.util.Util;

import com.threerings.flash.MediaContainer;
import com.threerings.flash.MenuUtil;

import com.threerings.msoy.client.ContextMenuProvider;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.WorldContext;

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
        return !(_desc is StaticMediaDesc);
    }

    // TODO: doc
    public function isBlocked () :Boolean
    {
        return Prefs.isMediaBlocked(_desc.getMediaId());
    }

    // TODO: doc
    public function toggleBlocked () :void
    {
        var nowBlocked :Boolean = !isBlocked();
        Prefs.setMediaBlocked(_desc.getMediaId(), nowBlocked);
        setIsBlocked(nowBlocked);
    }

    // from ContextMenuProvider
    public function populateContextMenu (ctx :WorldContext, menuItems :Array) :void
    {
        // TEMP: restrict blocking to support only, for now
        if (ctx.getMemberObject().tokens.isSupport() && isBlockable()) {
            var isBlocked :Boolean = isBlocked();
            // TODO: if there happens to be another bleepable MsoyMediaContainer
            // also under the mouse, we'll probably clobber each other's menu items.
            // There's no human-meaningful identifier we can inject in the string from just
            // the MediaDesc. Punting!
            menuItems.push(MenuUtil.createControllerMenuItem(
                Msgs.GENERAL.get(isBlocked ? "b.unbleep_media" : "b.bleep_media"),
                toggleBlocked));
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

    /** Our Media descriptor. */
    protected var _desc :MediaDesc;
}
}
