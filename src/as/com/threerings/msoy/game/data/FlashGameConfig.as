//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.crowd.client.PlaceController;

import com.threerings.msoy.data.MediaData;

import com.threerings.msoy.game.client.FlashGameConfigurator;
import com.threerings.msoy.game.client.FlashGameController;

/**
 * A game config for a simple multiplayer flash game.
 */
public class FlashGameConfig extends EZGameConfig
{
    /** The media that is the game we're going to play. */
    public var game :MediaData;

    override public function createController () :PlaceController
    {
        return new FlashGameController();
    }

    override public function createConfigurator () :GameConfigurator
    {
        return new FlashGameConfigurator();
    }

    override public function equals (other :Object) :Boolean
    {
        if (!super.equals(other)) {
            return false;
        }

        var that :FlashGameConfig = (other as FlashGameConfig);
        return this.game.equals(that.game);
    }

    override public function hashCode () :int
    {
        return super.hashCode() + game.hashCode()
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeObject(game);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        game = (ins.readObject() as MediaData);
    }
}
}
