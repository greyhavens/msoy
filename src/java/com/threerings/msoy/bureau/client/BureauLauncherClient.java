//
// $Id$

package com.threerings.msoy.bureau.client;

import com.threerings.presents.client.BlockingCommunicator;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.Communicator;
import com.threerings.presents.data.ClientObject;

import com.threerings.msoy.bureau.data.BureauLauncherCodes;
import com.threerings.msoy.bureau.data.BureauLauncherCredentials;
import com.threerings.msoy.bureau.server.BureauLauncherConfig;

/**
 * Connects to an msoy world or game server and dispatches requests to launch bureaus
 * back to a {@link BureauLauncher}. Also requests the game server registry and dispatches
 * the contents back to a connections objects.
 */
public class BureauLauncherClient extends Client
{
    /**
     * Creates a new bureau launcher client, setting up bureau launcher authentation.
     *
     * @param launcher used to run client jobs and receiver for launcher requests
     *
     * @see BureauLauncherCredentials
     * @see BureauLauncherConfig#bureauSharedSecret
     */
    public BureauLauncherClient (BureauLauncher launcher)
    {
        super(new BureauLauncherCredentials(
            BureauLauncherConfig.serverHost,
            BureauLauncherConfig.bureauSharedSecret), launcher.getRunner());

        _launcher = launcher;
        addServiceGroup(BureauLauncherCodes.BUREAU_LAUNCHER_GROUP);
        BureauLauncherReceiver receiver = new BureauLauncherReceiver () {
            public void launchThane (String bureauId, String token) {
                _launcher.launchThane(bureauId, token, getHostname(), getPorts()[0]);
            }
            public void shutdownLauncher () {
                _launcher.shutdownLauncher();
            }
            public void requestInfo (String hostname, int port) {
                _launcher.requestInfo(hostname, port);
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
    }

    protected BureauLauncherService _service;
    protected BureauLauncher _launcher;
}

