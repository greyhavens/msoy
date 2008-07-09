//
// $Id$

package com.threerings.msoy.bureau.client;

import com.threerings.msoy.bureau.data.BureauLauncherCredentials;
import com.threerings.msoy.bureau.data.BureauLauncherCodes;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.presents.client.BlockingCommunicator;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.Communicator;
import com.threerings.presents.data.ClientObject;

import static com.threerings.msoy.Log.log;

/**
 * Connects to the msoy game server and receives requests to launch bureaus.
 */
public class BureauLauncherClient extends Client
{
    /**
     * Creates a new bureau launcher client, setting up bureau launcher authentation.
     * @see BureauLauncherCredentials
     * @see BureauLauncherAuthenticator
     * @see ServerConfig#bureauSharedSecret
     */
    public BureauLauncherClient (BureauLauncher launcher)
    {
        super(new BureauLauncherCredentials(
            ServerConfig.serverHost, 
            ServerConfig.bureauSharedSecret), launcher.getRunner());

        addServiceGroup(BureauLauncherCodes.BUREAU_LAUNCHER_GROUP);
        getInvocationDirector().registerReceiver(new BureauLauncherDecoder(launcher));
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
}
