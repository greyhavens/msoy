//
// $Id$

package com.threerings.msoy.web.client;

import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.gwt.ItemDetail;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import com.threerings.msoy.web.data.TagHistory;

/**
 * Provides digital items related services.
 */
public interface ItemService extends RemoteService
{
    /** Provides results for {@link #loadItemDetail}. */
    public static class DetailOrIdent implements IsSerializable
    {
        public DetailOrIdent ()
        {
        }

        public DetailOrIdent (ItemDetail detail, ItemIdent ident)
        {
            this.detail = detail;
            this.ident = ident;
        }

        /** Set if the user has access to the item in question. */
        public ItemDetail detail;

        /** Else, this is set.  Only one of detail and ident will be non-null. */
        public ItemIdent ident;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/itemsvc";

    /**
     * Requests that the supplied item be created and inserted into the creating user's inventory.
     *
     * @return the newly created item.
     *
     * @exception ServiceException thrown if there is any problem creating the item.
     */
    public Item createItem (WebIdent ident, Item item, ItemIdent parent)
        throws ServiceException;

    /**
     * Requests that the supplied item be updated based on user provided changes.
     *
     * @exception ServiceException thrown if there is a problem updating the item.
     */
    public void updateItem (WebIdent ident, Item item)
        throws ServiceException;

    /**
     * Requests that the supplied item be remixed to the new media contained within it.
     */
    public Item remixItem (WebIdent ident, Item item)
        throws ServiceException;

    /**
     * Revert the specified clone back its default media.
     */
    public Item revertRemixedClone (WebIdent ident, ItemIdent itemIdent)
        throws ServiceException;

    /**
     * Rename the specified clone.
     * @param name the new name, or if blank, revert to the original name.
     */
    public String renameClone (WebIdent ident, ItemIdent itemIdent, String name)
        throws ServiceException;

    /**
     * Loads the details of a particular item.
     */
    public Item loadItem (WebIdent ident, ItemIdent item)
        throws ServiceException;

    /**
     * Loads the detailed details of a particular item.
     */
    public DetailOrIdent loadItemDetail (WebIdent ident, ItemIdent item)
        throws ServiceException;

    /**
     * Update the persisted scale of an avatar.
     */
    public void scaleAvatar (WebIdent ident, int avatarId, float newScale)
        throws ServiceException;

    /**
     * Deletes an item from the caller's inventory.
     */
    public void deleteItem (WebIdent ident, ItemIdent item)
        throws ServiceException;

    /**
     * Awards an item a rating from 1 to 5.
     *
     * @return the new average rating for the item.
     */
    public float rateItem (WebIdent ident, ItemIdent item, byte rating)
        throws ServiceException;

    /**
     * Fetches the tags associated with an item.
     */
    public Collection<String> getTags (WebIdent ident, ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the tagging history for a given item.
     */
    public Collection<TagHistory> getTagHistory (WebIdent ident, ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the recently used tags for the calling member.
     */
    public Collection<TagHistory> getRecentTags (WebIdent ident)
        throws ServiceException;

    /**
     * Associates or disassociates a tag with an item.
     */
    public TagHistory tagItem (WebIdent ident, ItemIdent item, String tag, boolean set)
        throws ServiceException;

    /**
     * Wraps an item up as a gift, i.e. clears its ownership. If 'wrap' is false, we unwrap the
     * item instead (settings its owner to the unwrapper).
     */
    public void wrapItem (WebIdent ident, ItemIdent item, boolean wrap)
        throws ServiceException;

    /**
     * Atomically sets or clears one or more flags on an item.
     */
    public void setFlags (WebIdent ident, ItemIdent item, byte mask, byte values)
        throws ServiceException;

    /**
     * Designates the given item mature content or not.
     */
    public void setMature (WebIdent ident, ItemIdent item, boolean value)
        throws ServiceException;

    /**
     * Fetches the first 'count' items flagged as mature or copyright in the database.
     */
    public List<ItemDetail> getFlaggedItems (WebIdent ident, int count)
        throws ServiceException;

    /**
     * Deletes an item and notifies people who care with the given message.  If the item is listed
     * in the catalog, also delists it and deletes any clones.
     */
    public Integer deleteItemAdmin (WebIdent ident, ItemIdent item, String subject, String body)
        throws ServiceException;

}
