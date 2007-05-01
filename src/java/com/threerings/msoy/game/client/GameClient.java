//
// $Id$

package com.threerings.msoy.game.client;

import java.util.logging.Level;

import com.samskivert.servlet.user.Password;

import com.threerings.util.Name;

import com.threerings.presents.net.Credentials;
import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.toybox.client.ToyBoxClient;

import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.game.data.MsoyGameConfig;

import static com.threerings.msoy.Log.log;

/**
 * Customizes the standard ToyBox client with MetaSOY bits.
 */
public class GameClient extends ToyBoxClient
{
    @Override // from ToyBoxClient
    public Credentials createCredentials (String username, Password pw)
    {
        return new MsoyCredentials(new Name(username), pw);
    }

    @Override // from ToyBoxClient
    protected LocationDirector createLocationDirector ()
    {
        return new GameLocationDirector();
    }

    /** Handles various Whirled specific game client stuff. */
    protected class GameLocationDirector extends LocationDirector
    {
        public GameLocationDirector () {
            super(GameClient.this._ctx);
        }

        @Override
        public boolean moveBack () {
            log.info("TODO!");
            return false;
        }

        @Override
        protected PlaceController createController (PlaceConfig config) {
            if (config instanceof MsoyGameConfig) {
                MsoyGameConfig gconfig = (MsoyGameConfig)config;
                String ccls = gconfig.getGameDefinition().controller;
                try {
                    ClassLoader loader = _toydtr.getClassLoader(gconfig);
                    return (PlaceController)Class.forName(ccls, true, loader).newInstance();
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to instantiate game controller " +
                            "[class=" + ccls + "]", e);
                    return null;
                }
            } else {
                return super.createController(config);
            }
        }
    };
}
