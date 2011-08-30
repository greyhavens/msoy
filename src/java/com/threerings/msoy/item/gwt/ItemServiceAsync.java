//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.RatingHistoryResult;
import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListQuery;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.web.gwt.TagHistory;

/**
 * Provides the asynchronous version of {@link ItemService}.
 */
public interface ItemServiceAsync
{
    /**
     * The async version of {@link ItemService#getTagHistory}.
     */
    void getTagHistory (ItemIdent item, AsyncCallback<List<TagHistory>> callback);

    /**
     * The async version of {@link ItemService#getTags}.
     */
    void getTags (ItemIdent item, AsyncCallback<List<String>> callback);

    /**
     * The async version of {@link ItemService#addFlag}.
     */
    void addFlag (ItemIdent item, ItemFlag.Kind kind, String comment, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ItemService#rateItem}.
     */
    void rateItem (ItemIdent item, byte rating, AsyncCallback<RatingResult> callback);

    /**
     * The async version of {@link ItemService#getRatingHistory}.
     */
    void getRatingHistory (ItemIdent item, AsyncCallback<RatingHistoryResult> callback);

    /**
     * The async version of {@link ItemService#removeAllFlags}.
     */
    void removeAllFlags (ItemIdent iitem, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ItemService#scaleAvatar}.
     */
    void scaleAvatar (int avatarId, float newScale, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ItemService#tagItem}.
     */
    void tagItem (ItemIdent item, String tag, boolean set, AsyncCallback<TagHistory> callback);

    /**
     * The async version of {@link ItemService#setMature}.
     */
    void setMature (ItemIdent item, boolean value, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ItemService#setFavorite}.
     */
    void setFavorite (MsoyItemType itemType, int catalogId, boolean favorite, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ItemService#loadItemList}.
     */
    void loadItemList (ItemListQuery query, AsyncCallback<ItemService.ItemListResult> callback);

    /**
     * The async version of {@link ItemService#loadLineups}.
     */
    void loadLineups (int avatarId, AsyncCallback<GroupName[]> callback);

    /**
     * The async version of {@link ItemService#stampItem}.
     */
    void stampItem (ItemIdent ident, int groupId, boolean doStamp, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ItemService#setAvatarInLineup}.
     */
    void setAvatarInLineup (int catalogId, int groupId, boolean doAdd, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ItemService#complainTag}.
     */
    void complainTag (ItemIdent ident, String tag, String reason, AsyncCallback<Void> callback);
}
