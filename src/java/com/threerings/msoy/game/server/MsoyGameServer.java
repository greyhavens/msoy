//
// $Id$

package com.threerings.msoy.game.server;

import java.util.Map;
import java.util.logging.Level;

import com.google.common.collect.Maps;

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

import com.threerings.admin.server.ConfigRegistry;
import com.threerings.admin.server.DatabaseConfigRegistry;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.whirled.server.SceneRegistry;

import com.threerings.ezgame.server.DictionaryManager;

import com.threerings.parlor.game.server.GameManager;
import com.threerings.parlor.server.ParlorManager;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyBaseServer;

import com.threerings.msoy.item.server.persist.AvatarRepository;

import com.threerings.msoy.game.data.PlayerObject;

import static com.threerings.msoy.Log.log;

/**
 * A server that does nothing but host games.
 */
public class MsoyGameServer extends MsoyBaseServer
{
    /** The parlor manager in operation on this server. */
    public static ParlorManager parlorMan = new ParlorManager();

    /** Manages lobbies and other game bits on this server. */
    public static GameGameRegistry gameReg = new GameGameRegistry();

    /** Handles sandboxed game server code. */
    public static HostedGameManager hostedMan = new HostedGameManager();

    /** Manages our connection back to our parent world server. */
    public static WorldServerClient worldClient = new WorldServerClient();

    /** Used to load avatars when players log onto this game server. */
    public static AvatarRepository avatarRepo;

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
        if (args.length < 2) {
            System.err.println("Usage: MsoyGameServer listenPort connectPort");
            System.exit(-1);
        }

        // set up the proper logging services
        com.samskivert.util.Log.setLogProvider(new LoggingLogProvider());
        OneLineLogFormatter.configureDefaultHandler();

        MsoyGameServer server = new MsoyGameServer();
        try {
            server._listenPort = Integer.parseInt(args[0]);
            server._connectPort = Integer.parseInt(args[1]);
            server.init();
            server.run();

        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to initialize server", e);
            System.exit(255);
        }
    }

    /**
     * Returns the player object for the user identified by the given ID if they are resolved
     * on this game server currently, null otherwise. This must only be called from the dobjmgr
     * thread.
     */
    public static PlayerObject lookupPlayer (int playerId)
    {
        // We can't look up guests this way, as they all have the same memberId
        if (playerId == MemberName.GUEST_ID) {
            return null;
        }
        // MemberName.equals and hashCode only depend on the id
        return lookupPlayer(new MemberName(null, playerId));
    }

    /**
     * Returns the player object for the user identified by the given name if they are resolved
     * on this game server currently, null otherwise. This must only be called from the dobjmgr
     * thread.
     */
    public static PlayerObject lookupPlayer (MemberName name)
    {
        requireDObjThread();
        return _online.get(name);
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

        // intialize various services
        parlorMan.init(invmgr, plreg);
        gameReg.init(omgr, invmgr, perCtx, ratingRepo, _eventLog);
        avatarRepo = new AvatarRepository(perCtx);

        GameManager.setUserIdentifier(new GameManager.UserIdentifier() {
            public int getUserId (BodyObject bodyObj) {
                return ((PlayerObject) bodyObj).getMemberId(); // will return 0 for guests
            }
        });
        DictionaryManager.init("data/dictionary");

        // connect back to our parent world server
        worldClient.init(this, _listenPort, _connectPort);

        log.info("Game server initialized.");
    }

    @Override // from MsoyBaseServer
    protected ConfigRegistry createConfigRegistry ()
        throws Exception
    {
        // TODO: the game servers probably need to hear about changes to runtime config bits
        return new DatabaseConfigRegistry(perCtx, invoker);
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

    /** The port on which we connect back to our parent server. */
    protected int _connectPort;

    /** A mapping from member name to member object for all online members. */
    protected static Map<MemberName,PlayerObject> _online = Maps.newHashMap();
}
