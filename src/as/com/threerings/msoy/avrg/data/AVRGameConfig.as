//
// $Id: AVRGameConfig.java 9198 2008-05-16 19:21:43Z jamie $

package com.threerings.msoy.avrg.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;
import com.whirled.game.data.GameDefinition;
import com.threerings.msoy.avrg.client.AVRGameController;
import com.threerings.msoy.item.data.all.Game;

/**
 * Configuration for an AVR game. This is basically BaseGameConfig + MsoyGameConfig,
 * but without the GameConfig dependency.
 */
public class AVRGameConfig extends PlaceConfig
{
    /** The creator provided name of this game. */
    public var name :String;

    /**
     * Configures this config with information from the supplied {@link Game} item.
     */
    public function init (game :Game, gameDef :GameDefinition) :void
    {
        this.name = game.name;
        _gameId = game.gameId;
        _gameDef = gameDef;
    }

    // from PlaceConfig
    override public function createController () :PlaceController
    {
        return new AVRGameController();
    }

    /**
     * Returns the non-changing metadata that defines this game.
     */
    public function getGameDefinition () :GameDefinition
    {
        return _gameDef;
    }

    /**
     * Returns the gameId of this game.
     */
    public function getGameId () :int
    {
        return _gameId;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        name = ins.readField(String) as String;
        _gameId = ins.readInt();
        _gameDef = ins.readObject() as GameDefinition;
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(name);
        out.writeInt(_gameId);
        out.writeObject(_gameDef);
    }

    /** Our game's unique id. */
    protected var _gameId :int;

    /** Our game definition. */
    protected var _gameDef :GameDefinition;
}
}
