//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListQuery;
import com.threerings.msoy.item.data.all.Photo;

import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.TagHistory;

/**
 * Provides digital items related services.
 */
public interface ItemService extends RemoteService
{
    /** Provides results from {@link #loadItemList}. */
    public static class ItemListResult implements IsSerializable
    {
        /** The total number of items that would be returned for a query with no limit. */
        public int totalCount;

        /** The item results. */
        public List<Item> items;
    }

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
    float rateItem (ItemIdent item, byte rating, boolean isFirstRating)
        throws ServiceException;

    /**
     * Fetches the tags associated with an item.
     */
    List<String> getTags (ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the tagging history for a given item.
     */
    List<TagHistory> getTagHistory (ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the recently used tags for the calling member.
     */
    List<TagHistory> getRecentTags ()
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
     * Flags a item from the catalog as the current member's favorite (or not).
     */
    void setFavorite (byte itemType, int catalogId, boolean favorite)
        throws ServiceException;

    /**
     * Loads items from a list that match the given criteria.
     */
    ItemListResult loadItemList (ItemListQuery query) throws ServiceException;

    /**
     * Loads up all of this member's photo inventory. This exists separate from
     * StuffService.loadInventory because we want to allow photo selection in many places in the
     * website, but we don't want to have to compile in the entire Item hiearchy to do so.
     */
    List<Photo> loadPhotos ()
        throws ServiceException;
}
