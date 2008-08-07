//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous (client-side) version of {@link MeService}.
 */
public interface MeServiceAsync
{
    /**
     * The asynchronous version of {@link MeService#getMyWhirled}.
     */
    void getMyWhirled (AsyncCallback<MyWhirledData> callback);

    /**
     * The asynchronous version of {@link MeService#updateWhirledNews}.
     */
    void updateWhirledNews (String newsHtml, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link MeService#loadMyRooms}.
     */
    void loadMyRooms (AsyncCallback<List<MeService.Room>> callback);

    /**
     * The asynchronous version of {@link MeService#loadFeed}.
     */
    void loadFeed (int cutoffDays, AsyncCallback<List<FeedMessage>> callback);

    /**
     * The asynchronous version of {@link MeService#loadBadges}.
     */
    void loadBadges (AsyncCallback<PassportData> callback);
}
