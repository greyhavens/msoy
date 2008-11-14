//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.web.gwt.TagHistory;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListQuery;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.item.gwt.ItemService.ItemListResult;

/**
 * The asynchronous (client-side) version of {@link ItemService}.
 */
public interface ItemServiceAsync
{
    /**
     * The asynchronous version of {@link ItemService#scaleAvatar}.
     */
    void scaleAvatar (int avatarId, float newScale, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ItemService#rateItem}.
     */
    void rateItem (
        ItemIdent item, byte rating, AsyncCallback<RatingResult> callback);

    /**
     * The asynchronous version of {@link ItemService#getTags}.
     */
    void getTags (ItemIdent item, AsyncCallback<List<String>> callback);

    /**
     * The asynchronous versions of {@link ItemService#getTagHistory}.
     */
    void getTagHistory (ItemIdent item, AsyncCallback<List<TagHistory>> callback);

    /**
     * The asynchronous versions of {@link ItemService#getRecentTags}.
     */
    void getRecentTags (AsyncCallback<List<TagHistory>> callback);

    /**
     * The asynchronous version of {@link ItemService#tagItem}.
     */
    void tagItem (ItemIdent item, String tag, boolean set, AsyncCallback<TagHistory> callback);

    /**
     * The asynchronous version of {@link ItemService#setFlags}.
     */
    void setFlags (ItemIdent item, byte mask, byte values, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ItemService#setMature}.
     */
    void setMature (ItemIdent item, boolean value, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ItemService#setFavorite}.
     */
    void setFavorite (byte itemType, int catalogId, boolean favorite, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ItemService#loadPhotos}.
     */
    void loadPhotos (AsyncCallback<List<Photo>> callback);

    /**
     * Loads items from a list that match the given criteria.
     */
    void loadItemList (ItemListQuery query, AsyncCallback<ItemListResult> callback);
}
