//
// $Id$

package com.threerings.msoy.hood {

import com.adobe.serialization.json.*;

/**
 * Represents a single group in a neighborhood: its name and id, and its membership
 * count. We should probably also include its invitation policy.
 */
public class NeighborGroup extends Neighbor
    implements LogoHolder
{
    /** The group's id. */
    public var groupId :Number;

    /** The hash of the group's logo, if any. */
    public var groupLogo :String;

    /** The number of members in this group. */
    public var members :Number;

    /**
     * Instantiate and populate a {@link NeighborGroup} give a JSON configuration.
     */
    public static function fromJSON(JSON: Object) :NeighborGroup
    {
        var group: NeighborGroup = new NeighborGroup();
        if (JSON.name == null || JSON.id == null) {
            throw new Error("Missing name/id in JSON");
        }
        Neighbor.fromJSON(group, JSON);
        group.groupId = JSON.id;
        group.groupLogo = JSON.logo;
        group.members = JSON.members;
        return group;
    }

    // from LogoHolder
    public function getLogoHash () :String
    {
        return groupLogo;
    }
}
}
