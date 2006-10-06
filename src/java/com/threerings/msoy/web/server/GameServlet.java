//
// $Id$

package com.threerings.msoy.web.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.web.client.GameService;
import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link GameService}.
 */
public class GameServlet extends RemoteServiceServlet
    implements GameService
{
    // from interface GameService
    public LaunchConfig loadLaunchConfig (WebCreds creds, int gameId)
        throws ServiceException
    {
        // TODO: validate this user's creds

        // load up the metadata for this game
        ItemIdent ident = new ItemIdent(Item.GAME, gameId);
        ServletWaiter<Item> waiter =
            new ServletWaiter<Item>("loadItem[" + ident + "]");
        MsoyServer.itemMan.getItem(ident, waiter);
        Game game = (Game)waiter.waitForResult();
        if (game == null) {
            return null;
        }

        // create a launch config record for the game
        LaunchConfig config = new LaunchConfig();
        config.gameId = game.itemId;

        // TODO: support single player games
        switch (game.gameMedia.mimeType) {
        case MediaDesc.APPLICATION_SHOCKWAVE_FLASH:
            config.type = LaunchConfig.FLASH_LOBBIED;
            break;
        case MediaDesc.APPLICATION_JAVA_ARCHIVE:
            config.type = LaunchConfig.JAVA_LOBBIED;
            break;
        default:
            log.warning("Requested config for game of unknown media type " +
                        "[id=" + gameId + ", media=" + game.gameMedia + "].");
            return null;
        }

        config.resourceURL = "http://" + ServerConfig.serverHost + ":" +
            ServerConfig.getHttpPort() + "/media/"; // TODO
        config.gameMediaPath = game.gameMedia.getMediaPath();
        config.name = game.getDescription();
        config.server = ServerConfig.serverHost;
        config.port = ServerConfig.serverPorts[0];
        return config;
    }
}
