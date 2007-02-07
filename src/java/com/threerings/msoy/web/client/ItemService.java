//
// $Id$

package com.threerings.msoy.web.client;

import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemDetail;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

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
    public int createItem (WebCreds creds, Item item)
        throws ServiceException;

    /**
     * Requests that the supplied item be updated based on user provided
     * changes.
     *
     * @exception ServiceException thrown if there is a problem updating the
     * item.
     */
    public void updateItem (WebCreds creds, Item item)
        throws ServiceException;

    /**
     * Loads the details of a particular item.
     */
    public Item loadItem (WebCreds creds, ItemIdent item)
        throws ServiceException;

    /**
     * Loads the detailed details of a particular item.
     */
    public ItemDetail loadItemDetail (WebCreds creds, ItemIdent item)
        throws ServiceException;

    /**
     * Remixes a cloned item into a mutable original item.
     */
    public Item remixItem (WebCreds creds, ItemIdent item)
        throws ServiceException;

    /**
     * Deletes an item from the caller's inventory.
     */
    public void deleteItem (WebCreds creds, ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the rating somebody gave something, or 0.
     */
    public byte getRating (WebCreds creds, ItemIdent item, int memberId)
        throws ServiceException;

    /**
     * Awards an item a rating from 1 to 5.
     *
     * @return the new average rating for the item.
     */
    public float rateItem (WebCreds creds, ItemIdent item, byte rating)
        throws ServiceException;

    /**
     * Fetches the tags associated with an item.
     */
    public Collection getTags (WebCreds creds, ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the tagging history for a given item.
     */
    public Collection getTagHistory (WebCreds creds, ItemIdent item)
        throws ServiceException;

    /**
     * Fetches the recently used tags for the calling member.
     */
    public Collection getRecentTags (WebCreds creds)
        throws ServiceException;

    /**
     * Associates or disassociates a tag with an item.
     */
    public TagHistory tagItem (WebCreds creds, ItemIdent item, String tag, boolean set)
        throws ServiceException;
    
    /**
     * Atomically sets or clears one or more flags on an item.
     */
    public void setFlags (WebCreds creds, ItemIdent ident, byte mask, byte values)
        throws ServiceException;

    /**
     * Fetches the first 'count' items flagged as mature or copyright in the database.
     */
    public List getFlaggedItems (WebCreds creds, int count)
        throws ServiceException;

    /**
     * Deletes an item and notifies people who care with the given message.
     * If the item is listed in the catalog, also delists it and deletes any clones.
     * @throws ServiceException 
     */
    public Integer deleteItemAdmin (WebCreds creds, ItemIdent ident, String subject, String body)
        throws ServiceException;

}
