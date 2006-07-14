//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.util.logging.Level;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.util.AuditLogger;
import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;

import com.threerings.util.Name;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsClient;

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.server.WhirledServer;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.spot.server.SpotDispatcher;
import com.threerings.whirled.spot.server.SpotProvider;
import com.threerings.whirled.util.SceneFactory;

import com.threerings.msoy.world.data.RoomConfig;

import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.MsoySceneRepository;

import static com.threerings.msoy.Log.log;

/**
 * Msoy server class.
 */
public class MsoyServer extends WhirledServer
{
    /** The connection provider used to access our JDBC databases. */
    public static ConnectionProvider conProv;

    /** Contains information on our members. */
    public static MemberRepository memberRepo;

    /** The Msoy scene repository. */
    public static MsoySceneRepository sceneRepo;

    /** Provides spot-related services. */
    public static SpotProvider spotProv;

    /** Handles HTTP servlet requests. */
    public static MsoyHttpServer httpServer;

    @Override
    public void init ()
        throws Exception
    {
        // create our connection provider before calling super.init() because
        // our superclass will attempt to create our authenticator and we'll
        // need the connection provider ready at that time
        conProv = new StaticConnectionProvider(ServerConfig.getJDBCConfig());

        super.init();

        // set up the right client factory
        clmgr.setClientFactory(new ClientFactory() {
            public PresentsClient createClient (AuthRequest areq) {
                return new MsoyClient();
            }

            public ClientResolver createClientResolver (Name username) {
                return new MsoyClientResolver();
            }
        });

        // intialize various services
        spotProv = new SpotProvider(omgr, plreg, screg);
        invmgr.registerDispatcher(new SpotDispatcher(spotProv), true);
        sceneRepo = (MsoySceneRepository) _screp;

        // create and start up our HTTP server
        httpServer = new MsoyHttpServer();
        httpServer.init();

        log.info("Msoy server initialized.");
    }

    @Override
    public void shutdown ()
    {
        super.shutdown();

        // close our audit logs
        _glog.close();
        _ilog.close();
        _stlog.close();
    }

    /**
     * Creates an audit log with the specified name (which should not include
     * the <code>.log</code> suffix) in our server log directory.
     */
    public static AuditLogger createAuditLog (String logname)
    {
        return new AuditLogger(_logdir, logname + ".log");
    }

    /**
     * Loads a message to the general audit log.
     */
    public static void generalLog (String message)
    {
        _glog.log(message);
    }

    /**
     * Loads a message to the item audit log.
     */
    public static void itemLog (String message)
    {
        _ilog.log(message);
    }

    @Override
    protected SceneRepository createSceneRepository ()
        throws Exception
    {
        return new MsoySceneRepository(conProv);
    }

    @Override
    protected SceneFactory createSceneFactory ()
        throws Exception
    {
        return _sceneFactory;
    }

    @Override
    protected SceneRegistry.ConfigFactory createConfigFactory ()
        throws Exception
    {
        return _sceneFactory;
    }

    @Override
    protected Authenticator createAuthenticator ()
    {
        Authenticator auth = ServerConfig.getAuthenticator();
        if (auth != null) {
            return auth;
        }
        return super.createAuthenticator();
    }

    @Override
    protected int[] getListenPorts ()
    {
        return new int[] { 4010 };
    }

    @Override
    protected void logReport (String report)
    {
        _stlog.log(report);
    }

    public static void main (String[] args)
    {
        // set up the proper logging services
        com.samskivert.util.Log.setLogProvider(new LoggingLogProvider());
        OneLineLogFormatter.configureDefaultHandler();

        MsoyServer server = new MsoyServer();
        try {
            server.init();
            server.run();
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to initialize server", e);
        }
    }

    /** Our scene and config factory. */
    protected MsoySceneFactory _sceneFactory = new MsoySceneFactory();

    protected static File _logdir = new File(ServerConfig.serverRoot, "log");
    protected static AuditLogger _glog = createAuditLog("server");
    protected static AuditLogger _ilog = createAuditLog("item");
    protected static AuditLogger _stlog = createAuditLog("state");
}
