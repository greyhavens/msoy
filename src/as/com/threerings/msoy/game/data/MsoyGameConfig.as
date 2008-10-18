//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.crowd.client.PlaceController;

import com.whirled.game.data.GameDefinition;
import com.whirled.game.data.WhirledGameConfig;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.game.client.MsoyGameController;

/**
 * A game config for a simple multiplayer metasoy flash game.
 */
public class MsoyGameConfig extends WhirledGameConfig
{
    /** The game item. */
    public var game :Game;

    /** The game's groupId, or 0 for none. */
    public var groupId :int;

    public function MsoyGameConfig ()
    {
        // used for unserialization
    }

    /**
     * Configures this config with information from the supplied {@link Game} item.
     */
    public function init (game :Game, gameDef :GameDefinition, groupId :int) :void
    {
        this.game = game;
        this.groupId = groupId;
        _gameId = game.gameId;
        _gameDef = gameDef;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        game = Game(ins.readObject());
        groupId = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(game);
        out.writeInt(groupId);
    }

    // from BaseGameConfig
    override protected function createDefaultController () :PlaceController
    {
        return new MsoyGameController();
    }
}
}
