//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.toybox.client.ToyBoxApplet;
import com.threerings.toybox.client.ToyBoxClient;

/**
 * Holds the main Java interface to lobbying and launching (Java) games.
 */
public class GameApplet extends ToyBoxApplet
{
    @Override // from ToyBoxApplet
    protected ToyBoxClient createClient ()
    {
        return new GameClient();
    }
}
