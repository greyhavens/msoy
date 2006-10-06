//
// $Id$

package com.threerings.msoy.game.client;

import com.samskivert.util.StringUtil;

import com.threerings.presents.client.Client;

import com.threerings.toybox.client.ToyBoxApplet;
import com.threerings.toybox.client.ToyBoxClient;

import com.threerings.msoy.data.MsoyCredentials;

/**
 * Holds the main Java interface to lobbying and launching (Java) games.
 */
public class GameApplet extends ToyBoxApplet
{
    @Override // from ToyBoxApplet
    public void start ()
    {
        // if we have an authtoken we'll be auto-logging in, so don't display a
        // username and password
        String authtoken = getParameter("authtoken");
        if (!StringUtil.isBlank(authtoken)) {
            _client.getClientController().getLogonPanel().setAutoLoggingOn();
        }

        super.start();

        // now do our autologin if appropriate
        if (!StringUtil.isBlank(authtoken)) {
            MsoyCredentials creds = new MsoyCredentials();
            creds.sessionToken = authtoken;
            Client client = _client.getContext().getClient();
            client.setCredentials(creds);
            client.logon();
        }
    }

    @Override // from ToyBoxApplet
    protected ToyBoxClient createClient ()
    {
        return new GameClient();
    }
}
