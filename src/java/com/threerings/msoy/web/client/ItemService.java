//
// $Id$

package com.threerings.msoy.web.client;

import java.util.Collection;
import java.util.List;

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
    /**
     * Requests that the supplied item be created and inserted into the
     * creating user's inventory.
     *
     * @return the newly created item's id.
     *
     * @exception ServiceException thrown if there is any problem creating the
     * item.
     */
    public int createItem (WebIdent ident, Item item)
        throws ServiceException;

    /**
     * Requests that the supplied item be updated based on user provided
     * changes.
     *
     * @exception ServiceException thrown if there is a problem updating the
     * item.
     */
    public void updateItem (WebIdent ident, Item item)
        throws ServiceException;

    /**
     * Loads the details of a particular item.
     */
    public Item loadItem (WebIdent ident, ItemIdent item)
        throws ServiceException;

    /**
     * Loads the detailed details of a particular item.
     */
    public ItemDetail loadItemDetail (WebIdent ident, ItemIdent item)
        throws ServiceException;

    /**
     * Update the persisted scale of an avatar.
     */
    public void scaleAvatar (WebIdent ident, int avatarId, float newScale)
        throws ServiceException;

    /**
     * Remixes a cloned item into a mutable original item.
     */
    public Item remixItem (WebIdent ident, ItemIdent item)
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
     *
     * @gwt.typeArgs <java.lang.String>
     */
    public Collection getTags (WebIdent ident, ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the tagging history for a given item.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.TagHistory>
     */
    public Collection getTagHistory (WebIdent ident, ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the recently used tags for the calling member.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.TagHistory>
     */
    public Collection getRecentTags (WebIdent ident)
        throws ServiceException;

    /**
     * Associates or disassociates a tag with an item.
     */
    public TagHistory tagItem (WebIdent ident, ItemIdent item, String tag, boolean set)
        throws ServiceException;

    /**
     * Wraps an item up as a gift, i.e. clears its ownership. If 'wrap' is false, we
     * unwrap the item instead (settings its owner to the unwrapper).
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
     *
     * @gwt.typeArgs <com.threerings.msoy.item.data.gwt.ItemDetail>
     */
    public List getFlaggedItems (WebIdent ident, int count)
        throws ServiceException;

    /**
     * Deletes an item and notifies people who care with the given message.
     * If the item is listed in the catalog, also delists it and deletes any clones.
     * @throws ServiceException
     */
    public Integer deleteItemAdmin (WebIdent ident, ItemIdent item, String subject, String body)
        throws ServiceException;

}
