//
// $Id$

package com.threerings.msoy.bureau.client;

import com.threerings.msoy.bureau.data.BureauLauncherCredentials;
import com.threerings.msoy.bureau.data.BureauLauncherCodes;
import com.threerings.msoy.bureau.data.ServerRegistryObject;
import com.threerings.msoy.bureau.server.BureauLauncherConfig;
import com.threerings.presents.client.BlockingCommunicator;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.Communicator;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.dobj.Subscriber;

import static com.threerings.msoy.Log.log;
import static com.threerings.msoy.bureau.data.ServerRegistryObject.ServerInfo;

/**
 * Connects to an msoy world or game server and dispatches requests to launch bureaus
 * back to a {@link BureauLauncher}. Also requests the game server registry and dispatches
 * the contents back to a connections objects.
 * @see BureauLauncherService#getGameServerRegistryOid
 */
public class BureauLauncherClient extends Client
{
    /**
     * Creates a new bureau launcher client, setting up bureau launcher authentation.
     * 
     * @param launcher used to run client jobs and receiver for launcher requests
     * @param target if specified, this client will subscribe to the game server registry
     * object and connect to all the servers therein.
     * 
     * @see BureauLauncherCredentials
     * @see BureauLauncherConfig#bureauSharedSecret
     * @see ServerRegistryObject
     */
    public BureauLauncherClient (BureauLauncher launcher, Connections target)
    {
        super(new BureauLauncherCredentials(
            BureauLauncherConfig.serverHost,
            BureauLauncherConfig.bureauSharedSecret), launcher.getRunner());

        _target = target;
        _launcher = launcher;
        addServiceGroup(BureauLauncherCodes.BUREAU_LAUNCHER_GROUP);
        BureauLauncherReceiver receiver = new BureauLauncherReceiver () {
            public void launchThane (String bureauId, String token)
            {
                _launcher.launchThane(bureauId, token, getHostname(), getPorts()[0]);
            }

            public void shutdownLauncher ()
            {
                _launcher.shutdownLauncher();
            }
        };
        getInvocationDirector().registerReceiver(new BureauLauncherDecoder(receiver));
    }

    @Override // from Client
    protected Communicator createCommunicator ()
    {
        return new BlockingCommunicator(this);
    }

    @Override // from Client
    protected void gotClientObject (ClientObject clobj)
    {
        super.gotClientObject(clobj);
        _service = getService(BureauLauncherService.class);
        _service.launcherInitialized(BureauLauncherClient.this);
        if (_target != null) {
            _service.getGameServerRegistryOid(this, new InvocationService.ResultListener() {
                public void requestProcessed (Object result) {
                    subcribeToServerRegistry((Integer)result);
                }

                public void requestFailed (String cause) {
                    log.warning("Failed to get game server registry oid", "client", this, "cause", cause);
                }
            });
        }
    }

    /**
     * Subscribe to the given server registry object and add its servers when it becomes
     * available.
     */
    protected void subcribeToServerRegistry (int oid)
    {
        log.info("Subscribing to game server registry", "client", this, "oid", oid);
        if (oid != 0) {
            _omgr.subscribeToObject(oid, new Subscriber<ServerRegistryObject>() {
                public void objectAvailable (ServerRegistryObject registry) {
                    addServers(registry);
                }

                public void requestFailed (int oid, ObjectAccessException oae) {
                    log.warning("Could not subscribe", "oid", oid, oae);
                }
            });
        }
    }

    /**
     * Adds all the servers contained in a registry and subscribes to future additions.
     */
    protected void addServers (ServerRegistryObject registry)
    {
        log.info("Adding servers", "client", this, "registry", registry);
        for (ServerInfo sinfo : registry.servers) {
            add(sinfo);
        }
        registry.addListener(new SetAdapter<ServerInfo>() {
            public void entryAdded (EntryAddedEvent<ServerInfo> event) {
                add(event.getEntry());
            }
            // we shouldn't need to listen for removals since when a server is remvoed, it
            // will presumably disconnect us
        });
    }

    /**
     * Adds a server to the target connections object.
     */
    protected void add (ServerInfo server) {
        _target.add(server.hostName, server.port);
    }

    protected BureauLauncherService _service;
    protected Connections _target;
    protected BureauLauncher _launcher;
}

