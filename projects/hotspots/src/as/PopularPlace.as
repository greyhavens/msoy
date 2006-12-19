package {

import flash.display.LoaderInfo;

import com.adobe.serialization.json.*;

/**
 * Represents the neighborhood around a central member: that member's name and id,
 * and arrays of their friends and groups.
 */
public class PopularPlace
{
    public var name :String;
    public var population: int;

    /**
     * Instantiate and populate an array of {@link PopularPlace}s from JSON configuration
     * extracted from the LoaderInfo FlashVars parameter 'hotspots'.
     */
    public static function fromLoaderInfo(info :LoaderInfo) :Array
    {
        return arrayFromJSON(new JSONDecoder(info.parameters.hotspots).getObject());
    }

    /**
     * Instantiate and populate an array of {@link PopularPlace}s from JSON configuration.
     */
    public static function arrayFromJSON(JSON: Object) :Array
    {
        if (!(JSON is Array)) {
            throw new Error("Expecting JSON Array");
        }
        var jArr :Array = JSON as Array;
        var result :Array = new Array();
        for (var i :int = 0; i < jArr.length; i ++) {
            var place :PopularPlace;
            if (jArr[i].gameId) {
                place = PopularGame.fromJSON(jArr[i]);
            } else {
                place = PopularScene.fromJSON(jArr[i]);
            }
            result.push(place);
        }
        return result;
    }

    /**
     * Populate a {@link PopularPlace} instance from JSON.
     */
    public static function populateFromJSON(place :PopularPlace, JSON :Object) :void
    {
        if (JSON.name == null || JSON.pop == null) {
            throw new Error("Missing name/pop in JSON");
        }
        place.name = JSON.name;
        place.population = JSON.pop;
    }
}
}