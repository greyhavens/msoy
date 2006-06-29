//
// $Id$

package com.threerings.msoy.server;

import java.util.logging.Level;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;

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

import com.threerings.msoy.server.persist.MsoySceneRepository;

import static com.threerings.msoy.Log.log;

/**
 * Msoy server class.
 */
public class MsoyServer extends WhirledServer
{
    /** The connection provider used to access our JDBC databases. */
    public static ConnectionProvider conprov;

    /** The Msoy scene repository. */
    public static MsoySceneRepository sceneRep;

    /** Provides spot-related services. */
    public static SpotProvider spotprov;

    // documentation inherited
    public void init ()
        throws Exception
    {
        conprov = new StaticConnectionProvider(ServerConfig.getJDBCConfig());

        super.init();

        // set up the right client class
        clmgr.setClientClass(MsoyClient.class);
        clmgr.setClientResolverClass(MsoyClientResolver.class);

        // intialize various services
        spotprov = new SpotProvider(omgr, plreg, screg);
        invmgr.registerDispatcher(new SpotDispatcher(spotprov), true);
        sceneRep = (MsoySceneRepository) _screp;

        log.info("Msoy server initialized.");
    }

    // documentation inherited
    protected SceneRepository createSceneRepository ()
        throws Exception
    {
        return new MsoySceneRepository(conprov);
    }

    // documentation inherited
    protected SceneFactory createSceneFactory ()
        throws Exception
    {
        return _sceneFactory;
    }

    // documentation inherited
    protected SceneRegistry.ConfigFactory createConfigFactory ()
        throws Exception
    {
        return _sceneFactory;
    }

    // documentation inherited
    protected int[] getListenPorts ()
    {
        return new int[] { 4010 };
    }

    public static void main (String[] args)
    {
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
}
