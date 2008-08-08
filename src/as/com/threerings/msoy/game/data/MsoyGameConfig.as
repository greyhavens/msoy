//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.crowd.client.PlaceController;

import com.whirled.game.data.GameDefinition;
import com.whirled.game.data.WhirledGameConfig;

import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.game.client.MsoyGameController;

/**
 * A game config for a simple multiplayer metasoy flash game.
 */
public class MsoyGameConfig extends WhirledGameConfig
{
    /** The creator provided name of this game. */
    public var name :String;

    /** The game's thumbnail media. */
    public var thumbnail :MediaDesc;

    public function MsoyGameConfig ()
    {
        // used for unserialization
    }

    /**
     * Configures this config with information from the supplied {@link Game} item.
     */
    public function init (game :Game, gameDef :GameDefinition) :void
    {
        this.name = game.name;
        this.thumbnail = game.getThumbnailMedia();
        _gameId = game.gameId;
        _gameDef = gameDef;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        name = (ins.readField(String) as String);
        thumbnail = (ins.readObject() as MediaDesc)
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(name);
        out.writeObject(thumbnail);
    }

    // from BaseGameConfig
    override protected function createDefaultController () :PlaceController
    {
        return new MsoyGameController();
    }
}
}
