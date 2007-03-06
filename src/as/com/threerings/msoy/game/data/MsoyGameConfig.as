//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.crowd.client.PlaceController;

import com.threerings.parlor.client.TableConfigurator;
import com.threerings.parlor.client.DefaultFlexTableConfigurator;

import com.threerings.parlor.game.client.GameConfigurator;
import com.threerings.parlor.game.client.FlexGameConfigurator;

import com.threerings.ezgame.data.EZGameConfig;

import com.threerings.msoy.game.client.MsoyGameController;

/**
 * A game config for a simple multiplayer metasoy flash game.
 */
public class MsoyGameConfig extends EZGameConfig
{
    /** The name of our game. */
    public var name :String;

    override public function createConfigurator () :GameConfigurator
    {
        return new FlexGameConfigurator();
    }

    override public function createController () :PlaceController
    {
        return new MsoyGameController();
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        name = (ins.readField(String) as String);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(name);
    }
}
}
