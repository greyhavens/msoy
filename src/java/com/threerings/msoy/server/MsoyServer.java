//
// $Id$

package com.threerings.msoy.server;

import java.util.logging.Level;

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

import com.threerings.msoy.data.SimpleChatConfig;
import com.threerings.msoy.data.RoomConfig;

import com.threerings.msoy.server.persist.MsoySceneRepository;

import static com.threerings.msoy.Log.log;

/**
 * Msoy server class.
 */
public class MsoyServer extends WhirledServer
{
    /** The oid of the global chat room. */
    public static int chatOid;

    /** The Msoy scene repository. */
    public static MsoySceneRepository sceneRep;

    /** Provides spot-related services. */
    public static SpotProvider spotprov;

    // documentation inherited
    public void init ()
        throws Exception
    {
        super.init();

        // set up the right client class
        clmgr.setClientClass(MsoyClient.class);

        // intialize various services
        spotprov = new SpotProvider(omgr, plreg, screg);
        invmgr.registerDispatcher(new SpotDispatcher(spotprov), true);
        sceneRep = (MsoySceneRepository) _screp;

        // create the global chat place
        plreg.createPlace(new RoomConfig(),
            new PlaceRegistry.CreationObserver() {
                public void placeCreated (PlaceObject place, PlaceManager plmgr)
                {
                    chatOid = place.getOid();
                }
            });

        log.info("Msoy server initialized.");
    }

    // documentation inherited
    protected SceneRepository createSceneRepository ()
        throws Exception
    {
        return new MsoySceneRepository(null);
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
