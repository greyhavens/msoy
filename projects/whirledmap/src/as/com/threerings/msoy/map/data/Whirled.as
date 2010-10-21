//
// $Id$

package com.threerings.msoy.map.data {

import com.adobe.serialization.json.*;

import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.MediaDescBase;
import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.data.all.StaticMediaDesc;
import com.threerings.util.Random;
import com.threerings.util.RandomUtil;

/**
 * Represents something in a neighborhood: currently either a friend or a group.
 */
public class Whirled
{
    /** The id of this whirled's group. */
    public var groupId :int;

    /** The name of this place. */
    public var name :String;

    /** The whirled's logo. */
    public var logo :MediaDescBase;

    /** The number of members occupying this whirled place. */
    public var population :int;

    /** The id of the scene the player ends up in if they click on this Whirled. */
    public var homeId :int;

    /**
     * Instantiates an existing whirled subclass with data common to this superclass.
     */
    public static function fromJSON (json: Object) :Whirled
    {
        var whirled :Whirled = new Whirled();
        whirled.groupId = json.groupId;
        whirled.name = json.name;
        if (json.logoHash != null) {
            whirled.logo = new HashMediaDesc(
                HashMediaDesc.stringToHash(json.logoHash), int(json.logoType));
        } else {
            whirled.logo = new StaticMediaDesc(
                MediaMimeTypes.IMAGE_PNG, "photo", "group_logo");
        }
        whirled.population = json.pop;
        whirled.homeId = json.homeId;
        return whirled;
    }

    public static function debugWhirled () :Whirled
    {
        var whirled :Whirled = new Whirled();
        var bit :Array = RandomUtil.pickRandom(DEBUG_WHIRLEDS) as Array;
        whirled.name = String(bit[0]);
        whirled.logo = new HashMediaDesc(HashMediaDesc.stringToHash(String(bit[1])), 10);
        whirled.population = rnd.nextInt(1000);
        return whirled;
    }

    protected static var rnd :Random = new Random();

    protected static const DEBUG_WHIRLEDS :Array = [
        [ "Monster Rocks", "b61d047ed2172e196b3b089aa25939ee3cf6bff4" ],
        [ "OOO Whirled", "d11d3dd33f89a6f6e4caa7d9fcee9d60c86bd24c" ],
        [ "Majestically Grandiose Tree House Defense", "f5cb77cf022cc759af25ec8c792a5d7d8a42151a" ],
        [ "Bug Hunters", "994eaa7909d2e17e2fa4a7f39c6ef4fee4fc1acf" ],
        [ "Ghosthunters", "aab40f60c917807c0b4713de6d5d2099a469839d" ],
    ];
}
}
