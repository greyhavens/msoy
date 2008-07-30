//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.Collection;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import com.threerings.msoy.web.data.TagHistory;

/**
 * Provides digital items related services.
 */
public interface ItemService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/itemsvc";

    /**
     * Update the persisted scale of an avatar.
     */
    void scaleAvatar (WebIdent ident, int avatarId, float newScale)
        throws ServiceException;

    /**
     * Awards an item a rating from 1 to 5.
     *
     * @return the new average rating for the item.
     */
    float rateItem (WebIdent ident, ItemIdent item, byte rating)
        throws ServiceException;

    /**
     * Wraps an item up as a gift, i.e. clears its ownership. If 'wrap' is false, we unwrap the
     * item instead (settings its owner to the unwrapper).
     */
    void wrapItem (WebIdent ident, ItemIdent item, boolean wrap)
        throws ServiceException;

    /**
     * Fetches the tags associated with an item.
     */
    Collection<String> getTags (WebIdent ident, ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the tagging history for a given item.
     */
    Collection<TagHistory> getTagHistory (WebIdent ident, ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the recently used tags for the calling member.
     */
    Collection<TagHistory> getRecentTags (WebIdent ident)
        throws ServiceException;

    /**
     * Associates or disassociates a tag with an item.
     */
    TagHistory tagItem (WebIdent ident, ItemIdent item, String tag, boolean set)
        throws ServiceException;

    /**
     * Atomically sets or clears one or more flags on an item.
     */
    void setFlags (WebIdent ident, ItemIdent item, byte mask, byte values)
        throws ServiceException;

    /**
     * Designates the given item mature content or not.
     */
    void setMature (WebIdent ident, ItemIdent item, boolean value)
        throws ServiceException;

    /**
     * Flags an item as the current member's favorite or not.
     */
    void setFavorite(WebIdent ident, ItemIdent item, boolean favorite)
        throws ServiceException;
}
