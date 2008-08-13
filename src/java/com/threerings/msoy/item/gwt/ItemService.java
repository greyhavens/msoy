//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.web.data.ServiceException;

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
    void scaleAvatar (int avatarId, float newScale)
        throws ServiceException;

    /**
     * Awards an item a rating from 1 to 5.
     *
     * @return the new average rating for the item.
     */
    float rateItem (ItemIdent item, byte rating, boolean previouslyRate)
        throws ServiceException;

    /**
     * Wraps an item up as a gift, i.e. clears its ownership. If 'wrap' is false, we unwrap the
     * item instead (settings its owner to the unwrapper).
     */
    void wrapItem (ItemIdent item, boolean wrap)
        throws ServiceException;

    /**
     * Fetches the tags associated with an item.
     */
    Collection<String> getTags (ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the tagging history for a given item.
     */
    Collection<TagHistory> getTagHistory (ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the recently used tags for the calling member.
     */
    Collection<TagHistory> getRecentTags ()
        throws ServiceException;

    /**
     * Associates or disassociates a tag with an item.
     */
    TagHistory tagItem (ItemIdent item, String tag, boolean set)
        throws ServiceException;

    /**
     * Atomically sets or clears one or more flags on an item.
     */
    void setFlags (ItemIdent item, byte mask, byte values)
        throws ServiceException;

    /**
     * Designates the given item mature content or not.
     */
    void setMature (ItemIdent item, boolean value)
        throws ServiceException;

    /**
     * Flags an item as the current member's favorite or not.
     */
    void setFavorite (ItemIdent item, boolean favorite)
        throws ServiceException;

    /**
     * Loads up all of this member's photo inventory. This exists separate from
     * StuffService.loadInventory because we want to allow photo selection in many places in the
     * website, but we don't want to have to compile in the entire Item hiearchy to do so.
     */
    List<Photo> loadPhotos ()
        throws ServiceException;
}
