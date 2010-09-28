//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.RatingHistoryResult;
import com.threerings.msoy.data.all.RatingResult;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListQuery;

import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.TagHistory;

/**
 * Provides digital items related services.
 */
@RemoteServiceRelativePath(ItemService.REL_PATH)
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

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + ItemService.ENTRY_POINT;

    /**
     * Update the persisted scale of an avatar.
     */
    void scaleAvatar (int avatarId, float newScale)
        throws ServiceException;

    /**
     * Awards an item a rating from 1 to 5.
     */
    RatingResult rateItem (ItemIdent item, byte rating)
        throws ServiceException;

    /**
     * Fetch the complete rating history of the specified item as a {@link RatingHistoryResult}.
     */
    RatingHistoryResult getRatingHistory (ItemIdent item)
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
     * Associates or disassociates a tag with an item.
     */
    TagHistory tagItem (ItemIdent item, String tag, boolean set)
        throws ServiceException;

    /**
     * Adds a user flag to an item, for subsequent review by support.
     */
    void addFlag (ItemIdent item, ItemFlag.Kind kind, String comment)
        throws ServiceException;

    /**
     * Removes all flags for an item, support only.
     */
    void removeAllFlags (ItemIdent iitem)
        throws ServiceException;

    /**
     * Designates the given item mature content or not.
     */
    void setMature (ItemIdent item, boolean value)
        throws ServiceException;

    /**
     * Flags a item from the catalog as the current member's favorite (or not).
     */
    void setFavorite (MsoyItemType itemType, int catalogId, boolean favorite)
        throws ServiceException;

    /**
     * Loads items from a list that match the given criteria.
     */
    ItemListResult loadItemList (ItemListQuery query)
        throws ServiceException;

    /**
     * Loads the themes whose lineups include the given avatar.
     */
    GroupName[] loadLineups(int avatarId)
        throws ServiceException;

    /**
     * Stamps the given item with a theme, or removes such a stamp. The item is typically a clone
     * or a listed master.
     */
    void stampItem (ItemIdent ident, int groupId, boolean doStamp)
        throws ServiceException;

    /**
     * Adds or removes the given avatar to the given theme's lineup.
     */
    void setAvatarInLineup (int catalogId, int groupId, boolean doAdd)
        throws ServiceException;

    /**
     * Enters a complaint into the support system about the given tag. The target of the complaint
     * is the user who most recently entered the tag, or the creator of the item if the tag was
     * copied from somewhere.
     */
    void complainTag (ItemIdent ident, String tag, String reason)
        throws ServiceException;
}
