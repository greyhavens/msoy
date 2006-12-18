package {

import com.adobe.serialization.json.*;

/**
 * Represents a single group in a neighborhood: its name and id, and its membership
 * count. We should probably also include its invitation policy.
 */
public class PopularScene extends PopularPlace
{
    /** The id of this scene. */
    public var sceneId :int;

    /**
     * Instantiate and populate a {@link PopularScene} give a JSON configuration.
     */
    public static function fromJSON(JSON: Object) :PopularScene
    {
        var place:PopularScene = new PopularScene();
        PopularPlace.populateFromJSON(place, JSON);
        if (JSON.sceneId == null) {
            throw new Error("Missing sceneId in JSON");
        }
        place.sceneId = JSON.sceneId;
        return place;
    }
}
}
