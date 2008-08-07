//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.WebIdent;


/**
 * The asynchronous (client-side) version of {@link MeService}.
 */
public interface MeServiceAsync
{
    /**
     * The asynchronous version of {@link MeService#getMyWhirled}.
     */
    void getMyWhirled (WebIdent ident, AsyncCallback<MyWhirledData> callback);

    /**
     * The asynchronous version of {@link MeService#updateWhirledNews}.
     */
    void updateWhirledNews (WebIdent ident, String newsHtml, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link MeService#loadMyRooms}.
     */
    void loadMyRooms (WebIdent ident, AsyncCallback<List<MeService.Room>> callback);

    /**
     * The asynchronous version of {@link MeService#loadFeed}.
     */
    void loadFeed (WebIdent ident, int cutoffDays, AsyncCallback<List<FeedMessage>> callback);

    /**
     * The asynchronous version of {@link MeService#loadBadges}.
     */
    void loadBadges (WebIdent ident, AsyncCallback<PassportData> callback);
}
