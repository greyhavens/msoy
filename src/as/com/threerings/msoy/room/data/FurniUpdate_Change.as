//
// $Id$

package com.threerings.msoy.room.data {

/**
 * Represents the change of furni in the room.
 */
public class FurniUpdate_Change extends FurniUpdate
{
    // from FurniUpdate
    override protected function doUpdate (model :MsoySceneModel) :void
    {
        model.removeFurni(data);
        model.addFurni(data);
    }
}
}
