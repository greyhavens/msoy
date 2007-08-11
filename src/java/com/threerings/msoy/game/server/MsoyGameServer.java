//
// $Id$

package com.threerings.msoy.game.server;

import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;

import com.threerings.util.Name;

import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsClient;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.server.CrowdServer;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.whirled.server.SceneRegistry;

import com.threerings.ezgame.server.DictionaryManager;

import com.threerings.parlor.game.server.GameManager;
import com.threerings.parlor.server.ParlorManager;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyBaseServer;

import com.threerings.msoy.item.server.persist.GameRepository;

import com.threerings.msoy.game.data.PlayerObject;

import static com.threerings.msoy.Log.log;

/**
 * A server that does nothing but host games.
 */
public class MsoyGameServer extends MsoyBaseServer
{
    /** Contains information on our games. */
    public static GameRepository gameRepo;

    /** The parlor manager in operation on this server. */
    public static ParlorManager parlorMan = new ParlorManager();

    /** Manages lobbies on this server. */
    public static LobbyRegistry lobbyReg = new LobbyRegistry();

    /** Handles sandboxed game server code. */
    public static HostedGameManager hostedMan = new HostedGameManager();

    /** Manages our connection back to our parent world server. */
    public static WorldServerClient worldClient = new WorldServerClient();

    /**
     * Called when a player starts their session to associate the name with the player's
     * distributed object.
     */
    public static void playerLoggedOn (PlayerObject plobj)
    {
        _online.put(plobj.memberName, plobj);
    }

    /**
     * Called when a player ends their session to clear their name to player object mapping.
     */
    public static void playerLoggedOff (PlayerObject plobj)
    {
        _online.remove(plobj.memberName);
    }

    /**
     * Returns true if this server is running, false if not.
     */
    public static boolean isActive ()
    {
        return (memberRepo != null);
    }

    /**
     * Starts everything a runnin'.
     */
    public static void main (String[] args)
    {
        if (args.length < 1) {
            System.err.println("Usage: MsoyGameServer port");
            System.exit(-1);
        }

        // set up the proper logging services
        com.samskivert.util.Log.setLogProvider(new LoggingLogProvider());
        OneLineLogFormatter.configureDefaultHandler();

        MsoyGameServer server = new MsoyGameServer();
        try {
            server._listenPort = Integer.parseInt(args[0]);
            server.init();
            server.run();

        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to initialize server", e);
            System.exit(255);
        }
    }

    @Override
    public void init ()
        throws Exception
    {
        super.init();

        // set up the right client factory
        clmgr.setClientFactory(new ClientFactory() {
            public PresentsClient createClient (AuthRequest areq) {
                return new MsoyGameClient();
            }
            public ClientResolver createClientResolver (Name username) {
                return new MsoyGameClientResolver();
            }
        });

        // create our various repositories
        gameRepo = new GameRepository(perCtx);

        // intialize various services
        parlorMan.init(invmgr, plreg);
        lobbyReg.init(omgr, invmgr, gameRepo);

        GameManager.setUserIdentifier(new GameManager.UserIdentifier() {
            public int getUserId (BodyObject bodyObj) {
                return ((PlayerObject) bodyObj).getMemberId(); // will return 0 for guests
            }
        });
        DictionaryManager.init("data/dictionary");

        // connect back to our parent world server
        worldClient.init(this, _listenPort);

        log.info("Game server initialized.");
    }

    @Override // from WhirledServer
    protected SceneRegistry createSceneRegistry ()
        throws Exception
    {
        return null; // not used
    }

    @Override // from CrowdServer
    protected PlaceRegistry createPlaceRegistry (InvocationManager invmgr, RootDObjectManager omgr)
    {
        return new PlaceRegistry(invmgr, omgr) {
            public ClassLoader getClassLoader (PlaceConfig config) {
                ClassLoader loader = hostedMan.getClassLoader(config);
                return (loader == null) ? super.getClassLoader(config) : loader;
            }
        };
    }

    @Override // from CrowdServer
    protected BodyLocator createBodyLocator ()
    {
        return new BodyLocator() {
            public BodyObject get (Name visibleName) {
                return _online.get(visibleName);
            }
        };
    }

    @Override // from PresentsServer
    protected Authenticator createAuthenticator ()
    {
        return new MsoyGameAuthenticator(memberRepo);
    }

    @Override // from PresentsServer
    protected int[] getListenPorts ()
    {
        return new int[] { _listenPort };
    }

    @Override // from PresentsServer
    protected void logReport (String report)
    {
        // no reports for game servers
    }

    /** The port on which we listen for client connections. */
    protected int _listenPort;

    /** A mapping from member name to member object for all online members. */
    protected static HashMap<MemberName,PlayerObject> _online =
        new HashMap<MemberName,PlayerObject>();
}
