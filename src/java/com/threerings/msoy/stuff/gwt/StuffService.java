//
// $Id$

package com.threerings.msoy.stuff.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemDetail;

/**
 * Provides services used by the stuff page.
 */
public interface StuffService extends RemoteService
{
    /** Provides results for {@link #loadItemDetail}. */
    public static class DetailOrIdent implements IsSerializable
    {
        /** Set if the user has access to the item in question. */
        public ItemDetail detail;

        /** Else, this is set.  Only one of detail and ident will be non-null. */
        public ItemIdent ident;

        public DetailOrIdent () {
        }

        public DetailOrIdent (ItemDetail detail, ItemIdent ident) {
            this.detail = detail;
            this.ident = ident;
        }
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/stuffsvc";

    /**
     * Requests that the supplied item be created and inserted into the creating user's inventory.
     *
     * @return the newly created item.
     *
     * @exception ServiceException thrown if there is any problem creating the item.
     */
    Item createItem (WebIdent ident, Item item, ItemIdent parent)
        throws ServiceException;

    /**
     * Requests that the supplied item be updated based on user provided changes.
     *
     * @exception ServiceException thrown if there is a problem updating the item.
     */
    void updateItem (WebIdent ident, Item item)
        throws ServiceException;

    /**
     * Requests that the supplied item be remixed to the new media contained within it.
     */
    Item remixItem (WebIdent ident, Item item)
        throws ServiceException;

    /**
     * Revert the specified clone back its default media.
     */
    Item revertRemixedClone (WebIdent ident, ItemIdent itemIdent)
        throws ServiceException;

    /**
     * Rename the specified clone.
     * @param name the new name, or if blank, revert to the original name.
     */
    String renameClone (WebIdent ident, ItemIdent itemIdent, String name)
        throws ServiceException;

    /**
     * Loads all items in a player's inventory of the specified type and optionally restricted to
     * the specified suite.
     */
    List<Item> loadInventory (WebIdent ident, byte type, int suiteId)
        throws ServiceException;

    /**
     * Loads the details of a particular item.
     */
    Item loadItem (WebIdent ident, ItemIdent item)
        throws ServiceException;

    /**
     * Loads the detailed details of a particular item.
     */
    DetailOrIdent loadItemDetail (WebIdent ident, ItemIdent item)
        throws ServiceException;

    /**
     * Deletes an item from the caller's inventory.
     */
    void deleteItem (WebIdent ident, ItemIdent item)
        throws ServiceException;
}
