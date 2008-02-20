//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link WorldService}.
 */
public interface WorldServiceAsync
{
    /**
     * The asynchronous version of {@link WorldService#serializePopularPlaces}.
     */
    public void serializePopularPlaces (WebIdent ident, int n, AsyncCallback callback);

    /**
     * The asynchronous version of {@link WorldService#getMyWhirled}.
     */
    public void getMyWhirled (WebIdent ident, AsyncCallback callback);

    /**
     * The asynchronous version of {@link WorldService#getWhatIsWhirled}.
     */
    public void getWhatIsWhirled (AsyncCallback callback);

    /**
     * The asynchronous version of {@link WorldService#updateWhirledNews}.
     */
    public void updateWhirledNews (WebIdent ident, String newsHtml, AsyncCallback callback);

    /**
     * The asynchronous version of {@link WorldService#loadFeed}.
     */
    public void loadFeed (WebIdent ident, int cutoffDays, AsyncCallback callback);

    /**
     * The asynchronous version of {@link WorldService#loadLaunchConfig}.
     */
    public void loadLaunchConfig (WebIdent ident, int gameId, AsyncCallback callback);
}
