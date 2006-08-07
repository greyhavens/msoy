//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.util.MessageBundle;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.parlor.game.client.GameConfigurator;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.data.MediaData;

import com.threerings.msoy.game.client.FlashGameConfigurator;

/**
 * A game config for a simple multiplayer flash game.
 */
public class FlashGameConfig extends GameConfig
{
    /** A creator-submitted name of the game. */
    public var gameName :String;

    /** The media that is the game we're going to play. */
    public var game :MediaData;

    override public function getBundleName () :String
    {
        return "general";
    }

    override public function createConfigurator () :GameConfigurator
    {
        return new FlashGameConfigurator();
    }

    override public function getGameName () :String
    {
        return MessageBundle.taint(gameName);
    }

    override public function equals (other :Object) :Boolean
    {
        if (!super.equals(other)) {
            return false;
        }

        var that :FlashGameConfig = (other as FlashGameConfig);
        return (this.gameName === that.gameName) && 
            (this.game === that.game);
    }

    override public function hashCode () :int
    {
        return super.hashCode() + game.hashCode()
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(gameName);
        out.writeObject(game);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        gameName = (ins.readField(String) as String);
        game = (ins.readObject() as MediaData);
    }
}
}
