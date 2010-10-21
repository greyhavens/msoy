//
// $Id$

package com.threerings.msoy.stuff.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.web.gwt.ServiceException;

import com.threerings.orth.data.MediaDesc;
import com.threerings.msoy.item.data.all.MsoyItemType;

import com.threerings.msoy.data.all.GroupName;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemDetail;

/**
 * Provides services used by the stuff page.
 */
@RemoteServiceRelativePath(StuffService.REL_PATH)
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

    /** Provides results for {@link #loadInventory}. */
    public static class InventoryResult<T> implements IsSerializable
    {
        /** The items constituting the inventory requested. */
        public List<T> items;

        /** The theme to which the inventory resolution was restricted. */
        public GroupName theme;

        public InventoryResult () {
        }

        public InventoryResult (List<T> items, GroupName theme) {
            this.items = items;
            this.theme = theme;
        }
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/stuffsvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + StuffService.ENTRY_POINT;

    /**
     * Publish the specified xml string as 'external media'.
     */
    MediaDesc publishExternalMedia (String data, byte mimeType)
        throws ServiceException;

    /**
     * Requests that the supplied item be created and inserted into the creating user's inventory.
     *
     * @return the newly created item.
     */
    Item createItem (Item item)
        throws ServiceException;

    /**
     * Requests that the supplied item be updated based on user provided changes.
     */
    void updateItem (Item item)
        throws ServiceException;

    /**
     * Requests that the supplied item be remixed to the new media contained within it.
     */
    Item remixItem (Item item)
        throws ServiceException;

    /**
     * Revert the specified clone back its default media.
     */
    Item revertRemixedClone (ItemIdent itemIdent)
        throws ServiceException;

    /**
     * Rename the specified clone.
     * @param name the new name, or if blank, revert to the original name.
     */
    String renameClone (ItemIdent itemIdent, String name)
        throws ServiceException;

    /**
     * Loads all items in a player's inventory of the specified type. If query is non-null, then
     * restrict the returned set to items whose name/description/tags match the string.
     */
    InventoryResult<Item> loadInventory (int memberId, MsoyItemType type, String query)
        throws ServiceException;

    /**
     * Loads the details of a particular item.
     */
    Item loadItem (ItemIdent item)
        throws ServiceException;

    /**
     * Loads the detailed details of a particular item.
     */
    DetailOrIdent loadItemDetail (ItemIdent item)
        throws ServiceException;

    /**
     * Deletes an item from the caller's inventory.
     */
    void deleteItem (ItemIdent item)
        throws ServiceException;

    /**
     * Loads a theme group's avatar lineup.
     */
    InventoryResult<Avatar> loadThemeLineup (int groupId)
        throws ServiceException;
}
