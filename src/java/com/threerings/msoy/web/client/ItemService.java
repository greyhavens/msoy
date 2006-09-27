//
// $Id$

package com.threerings.msoy.web.client;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.TagHistory;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

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
     * Loads all items in a player's inventory of the specified type.
     */
    public ArrayList loadInventory (WebCreds creds, String type)
        throws ServiceException;
    
    /** Remixes a cloned item into a fully mutable original item. */
    public Item remixItem (WebCreds creds, int itemId, String type)
        throws ServiceException;
    
    /** Fetches the rating somebody gave somebody, or 0. */
    public byte getRating (
        WebCreds creds, int itemId, String type, int memberId)
            throws ServiceException;
    
    /** Awards an item a rating from 1 to 5. */
    public Item rateItem (
        WebCreds creds, int itemId, String type, byte rating)
            throws ServiceException;
    
    /** Fetches the tagging history for a given item. */
    public Iterable getTagHistory (WebCreds creds, int itemId, String type)
            throws ServiceException;
    
    /** Associates a tag with an item. */
    public TagHistory tagItem (
        WebCreds creds, int itemId, String type, String tag)
            throws ServiceException;

    /** Disassociates a tag with an item. */
    public TagHistory untagItem (
        WebCreds creds, int itemId, String type, String tag)
            throws ServiceException;
}
