//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.crowd.client.PlaceController;

import com.whirled.game.data.GameDefinition;
import com.whirled.game.data.WhirledGameConfig;

import com.threerings.orth.data.MediaDesc;
import com.threerings.msoy.game.client.ParlorGameController;

/**
 * A game config for a simple multiplayer metasoy flash game.
 */
public class ParlorGameConfig extends WhirledGameConfig
    implements MsoyGameConfig
{
    /** Info on the game being played. */
    public var game :GameSummary;

    /** The game's groupId, or 0 for none. */
    public var groupId :int;

    /** The splash screen media, or null if we have none. */
    public var splashMedia :MediaDesc;

    public function ParlorGameConfig ()
    {
        // used for unserialization
    }

    /**
     * Configures this config with bare bones info.
     */
    public function init (game :GameSummary, gameDef :GameDefinition) :void
    {
        this.game = game;
        _gameId = game.gameId;
        _gameDef = gameDef;
    }

    // from interface MsoyGameConfig
    public function getName () :String
    {
        return game.name;
    }

    // from interface MsoyGameConfig
    public function getTextDescription () :String
    {
        return game.description;
    }

    // from interface MsoyGameConfig
    public function getThumbnail () :MediaDesc
    {
        return game.thumbMedia;
    }

    // from interface MsoyGameConfig
    public function getSplash () :MediaDesc
    {
        return splashMedia;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        game = GameSummary(ins.readObject());
        groupId = ins.readInt();
        splashMedia = MediaDesc(ins.readObject());
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(game);
        out.writeInt(groupId);
        out.writeObject(splashMedia);
    }

    // from BaseGameConfig
    override protected function createDefaultController () :PlaceController
    {
        return new ParlorGameController();
    }
}
}
