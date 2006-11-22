//
// $Id$

package com.threerings.msoy.web.client;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemDetail;
import com.threerings.msoy.item.web.ItemIdent;
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
     * Requests that the supplied item be updated based on user provided
     * changes.
     *
     * @exception ServiceException thrown if there is a problem updating the
     * item.
     */
    public void updateItem (WebCreds creds, Item item)
        throws ServiceException;

    /**
     * Loads all items in a player's inventory of the specified type.
     */
    public ArrayList loadInventory (WebCreds creds, byte type)
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
     * Fetches the tagging history for a given member.
     */
    public Collection getTagHistory (WebCreds creds, int memberId)
        throws ServiceException;

    /**
     * Associates a tag with an item.
     */
    public TagHistory tagItem (WebCreds creds, ItemIdent item, String tag)
        throws ServiceException;

    /**
     * Disassociates a tag with an item.
     */
    public TagHistory untagItem (WebCreds creds, ItemIdent item, String tag)
        throws ServiceException;
}
