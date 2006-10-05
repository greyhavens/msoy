//
// $Id$

package com.threerings.msoy.game.client;

import com.samskivert.servlet.user.Password;

import com.threerings.util.Name;

import com.threerings.presents.net.Credentials;

import com.threerings.toybox.client.ToyBoxClient;

import com.threerings.msoy.data.MsoyCredentials;

/**
 * Customizes the standard ToyBox client with MetaSOY bits.
 */
public class GameClient extends ToyBoxClient
{
    public Credentials createCredentials (String username, Password pw)
    {
        return new MsoyCredentials(new Name(username), pw);
    }
}
