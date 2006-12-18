package {

import com.adobe.serialization.json.*;

/**
 * Represents a single group in a neighborhood: its name and id, and its membership
 * count. We should probably also include its invitation policy.
 */
public class PopularGame extends PopularPlace
{
    /** The id of this game. */
    public var gameId :int;

    /**
     * Instantiate and populate a {@link PopularGame} give a JSON configuration.
     */
    public static function fromJSON(JSON: Object) :PopularGame
    {
        var place:PopularGame = new PopularGame();
        PopularPlace.populateFromJSON(place, JSON);
        if (JSON.gameId == null) {
            throw new Error("Missing gameId in JSON");
        }
        place.gameId = JSON.gameId;
        return place;
    }
}
}
