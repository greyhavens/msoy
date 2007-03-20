package com.threerings.msoy.hood {

import com.adobe.serialization.json.*;

/**
 * Represents a single game in a neighborhood: its name and id and logo.
 */
public class NeighborGame extends Neighbor
    implements LogoHolder
{
    /** The game's name. */
    public var gameName :String;
    /** The game's id. */
    public var gameId :Number;
    /** The hash of the game's logo, if any. */
    public var gameLogo :String;

    /**
     * Instantiate and populate a {@link NeighborGame} give a JSON configuration.
     */
    public static function fromJSON(JSON: Object) :NeighborGame
    {
        var game: NeighborGame = new NeighborGame();
        if (JSON.name == null || JSON.id == null) {
            throw new Error("Missing name/id in JSON");
        }
        Neighbor.fromJSON(game, JSON);
        game.gameName = JSON.name;
        game.gameId = JSON.id;
        game.gameLogo = JSON.logo;
        return game;
    }

    // from Neighbor
    override public function getName () :String
    {
        return gameName;
    }

    // from LogoHolder
    public function getLogoHash () :String
    {
        return gameLogo;
    }
}
}