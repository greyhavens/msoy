//
// $Id$

package com.threerings.msoy.map.data {

import com.adobe.serialization.json.*;

/**
 * Represents the neighborhood, potentially centered around one specific member or group.
 */
public class WhirledMap
{
    /** The member's groups, as {@link NeighborGroup} objects. */
    public var whirleds :Array = new Array();

    /**
     * Instantiate and populate a {@link WhirledMap} from JSON configuration
     * extracted from the LoaderInfo FlashVars parameter 'neighborhood'.
     */
    public static function fromParameters (data :String) :WhirledMap
    {
        return fromJSON(new JSONDecoder(data).getValue());
    }

    /**
     * Instantiate and populate a {@link WhirledMap} given a JSON configuration.
     */
    public static function fromJSON (json: Object) :WhirledMap
    {
        var map :WhirledMap = new WhirledMap();
        map.whirleds = new Array();

        if (json.themes != null) {
            for (var ii :int = 0; ii < json.themes.length; ii ++) {
                map.whirleds[ii] = Whirled.fromJSON(json.themes[ii]);
            }
        }
        return map;
    }

    public static function debugMap () :WhirledMap
    {
        var map :WhirledMap = new WhirledMap();
        map.whirleds = [
            Whirled.debugWhirled(),
            Whirled.debugWhirled(),
            Whirled.debugWhirled(),
            Whirled.debugWhirled(),
            Whirled.debugWhirled(),
            Whirled.debugWhirled()
        ];

        return map;
    }
}
}
