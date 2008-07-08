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

    /**
     * Set the server to connect to to the given server:port.
     */
    public void setServer (String serverNameAndPort)
    {
        int colon = serverNameAndPort.indexOf(':');
        if (colon == -1) {
            throw new Error("invalid config, no port number on " + serverNameAndPort);
        }
        String server = serverNameAndPort.substring(0, colon);
        String port = serverNameAndPort.substring(colon + 1);
        setServer(server, new int [] {Integer.parseInt(port)});
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
