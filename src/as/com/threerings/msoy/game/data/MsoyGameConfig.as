//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.util.ClassUtil;

import com.threerings.crowd.client.PlaceController;

import com.threerings.ezgame.data.GameDefinition;

import com.threerings.toybox.data.ToyBoxGameConfig;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.game.client.MsoyGameController;

/**
 * A game config for a simple multiplayer metasoy flash game.
 */
public class MsoyGameConfig extends ToyBoxGameConfig
{
    /** The creator provided name of this game. */
    public var name :String;

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
        _gameId = game.getPrototypeId();
        _gameDef = gameDef;
    }

    // from PlaceConfig
    override public function createController () :PlaceController
    {
        var controller :String = getGameDefinition().controller;
        if (controller == null) {
            return new MsoyGameController();
        }
        var c :Class = ClassUtil.getClassByName(controller);
        return (new c() as PlaceController);
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
