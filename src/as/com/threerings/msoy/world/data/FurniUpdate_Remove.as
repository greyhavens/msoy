//
// $Id$

package com.threerings.msoy.world.data {

/**
 * Represents the removal of furni from the room.
 */
public class FurniUpdate_Remove extends FurniUpdate
{
    // from FurniUpdate
    override protected function doUpdate (model :MsoySceneModel) :void
    {
        model.removeFurni(data);
    }
}
}
