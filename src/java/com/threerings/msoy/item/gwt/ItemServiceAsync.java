//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.TagHistory;
import com.threerings.msoy.web.data.WebIdent;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * The asynchronous (client-side) version of {@link ItemService}.
 */
public interface ItemServiceAsync
{
    /**
     * The asynchronous version of {@link ItemService#scaleAvatar}.
     */
    void scaleAvatar (WebIdent ident, int avatarId, float newScale,
                      AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ItemService#rateItem}.
     */
    void rateItem (WebIdent ident, ItemIdent item, byte rating, AsyncCallback<Float> callback);

    /**
     * The asynchronous version of {@link ItemService.wrapItem}.
     */
    void wrapItem (WebIdent ident, ItemIdent item, boolean wrap, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ItemService#getTags}.
     */
    void getTags (WebIdent ident, ItemIdent item, AsyncCallback<Collection<String>> callback);

    /**
     * The asynchronous versions of {@link ItemService#getTagHistory}.
     */
    void getTagHistory (WebIdent ident, ItemIdent item,
                        AsyncCallback<Collection<TagHistory>> callback);

    /**
     * The asynchronous versions of {@link ItemService#getRecentTags}.
     */
    void getRecentTags (WebIdent ident, AsyncCallback<Collection<TagHistory>> callback);

    /**
     * The asynchronous version of {@link ItemService#tagItem}.
     */
    void tagItem (WebIdent ident, ItemIdent item, String tag, boolean set,
                  AsyncCallback<TagHistory> callback);

    /**
     * The asynchronous version of {@link ItemService.setFlags}.
     */
    void setFlags (WebIdent ident, ItemIdent item, byte mask, byte values,
                   AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ItemService.setMature}.
     */
    void setMature (WebIdent ident, ItemIdent item, boolean value, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ItemService.setFavorite}.
     */
    void setFavorite (WebIdent ident, ItemIdent item, boolean favorite,
                      AsyncCallback<Void> callback);
}
