//
// $Id$

package com.threerings.msoy.web.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.StringUtil;

import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;

import com.threerings.toybox.xml.GameParser;
import com.threerings.toybox.server.persist.GameRecord;

import com.threerings.msoy.game.xml.MsoyGameParser;
import com.threerings.msoy.game.data.MsoyMatchConfig;

import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.web.client.GameService;
import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link GameService}.
 */
public class GameServlet extends MsoyServiceServlet
    implements GameService
{
    // from interface GameService
    public LaunchConfig loadLaunchConfig (WebCreds creds, int gameId)
        throws ServiceException
    {
        // TODO: validate this user's creds

        // load up the metadata for this game
        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(Item.GAME);
        ItemRecord itemRec;
        try {
            itemRec = repo.loadOriginalItem(gameId);
            if (itemRec == null) {
                return null;
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load game record [gameId=" + gameId + "]", pe);
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }
        final Game game = (Game)itemRec.toItem();

        // create a launch config record for the game
        LaunchConfig config = new LaunchConfig();
        config.gameId = game.itemId;

        MsoyMatchConfig match;
        try {
            if (StringUtil.isBlank(game.config)) {
                // fall back to a sensible default for our legacy games
                match = new MsoyMatchConfig();
                match.minSeats = match.startSeats = 1;
                match.maxSeats = 2;
            } else {
                match = (MsoyMatchConfig)(new GameRecord() {
                    { definition = game.config; } // anonymous constructor
                    protected GameParser createParser () {
                        return new MsoyGameParser();
                    }
                }).parseGameDefinition().match;
            }

        } catch (InvocationException ie) {
            log.log(Level.WARNING, "Failed to parse XML game definition [id=" + gameId + "]", ie);
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }

        switch (game.gameMedia.mimeType) {
        case MediaDesc.APPLICATION_SHOCKWAVE_FLASH:
            config.type = (game.isInWorld() ? LaunchConfig.FLASH_IN_WORLD :
                (match.maxSeats == 1 ? LaunchConfig.FLASH_SOLO : LaunchConfig.FLASH_LOBBIED));
            break;
        case MediaDesc.APPLICATION_JAVA_ARCHIVE:
            config.type = (match.maxSeats == 1 ?
                LaunchConfig.JAVA_SOLO : LaunchConfig.JAVA_LOBBIED);
            break;
        default:
            log.warning("Requested config for game of unknown media type " +
                        "[id=" + gameId + ", media=" + game.gameMedia + "].");
            return null;
        }

        config.resourceURL = "http://" + ServerConfig.serverHost + ":" +
            ServerConfig.getHttpPort() + "/media/"; // TODO
        config.gameMediaPath = game.gameMedia.getMediaPath();
        config.name = game.name;
        config.server = ServerConfig.serverHost;
        config.port = ServerConfig.serverPorts[0];
        return config;
    }
}
