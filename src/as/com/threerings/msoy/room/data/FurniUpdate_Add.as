//
// $Id$

package com.threerings.msoy.room.data {

/**
 * Represents the addition of furniture to a room.
 */
public class FurniUpdate_Add extends FurniUpdate
{
    // from FurniUpdate
    override protected function doUpdate (model :MsoySceneModel) :void
    {
        model.addFurni(data);
    }
}
}
