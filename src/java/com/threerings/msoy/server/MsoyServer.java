//
// $Id$

package com.threerings.msoy.server;

import java.util.logging.Level;

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.CrowdServer;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.msoy.data.SimpleChatConfig;

import static com.threerings.msoy.Log.log;

/**
 * Msoy server class.
 */
public class MsoyServer extends CrowdServer
{
    /** The oid of the global chat room. */
    public static int chatOid;

    // documentation inherited
    public void init ()
        throws Exception
    {
        super.init();

        // set up the right client class
        clmgr.setClientClass(MsoyClient.class);

        // create the global chat place
        plreg.createPlace(new SimpleChatConfig(),
            new PlaceRegistry.CreationObserver() {
                public void placeCreated (PlaceObject place, PlaceManager plmgr)
                {
                    chatOid = place.getOid();
                }
            });

        log.info("Msoy server initialized.");
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
}
