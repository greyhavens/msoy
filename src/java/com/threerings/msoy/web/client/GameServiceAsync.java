//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link GameService}.
 */
public interface GameServiceAsync
{
    /**
     * The asynchronous version of {@link GameService#loadLaunchConfig}.
     */
    public void loadLaunchConfig (WebIdent ident, int gameId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GameService#loadGameDetail}.
     */
    public void loadGameDetail (WebIdent ident, int gameId, AsyncCallback callback);
}
