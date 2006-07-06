//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;

/**
 * A scene update for msoy.
 */
public class ModifyFurniUpdate extends SceneUpdate
{
    /** The furniture removed from the scene (or null for none). */
    public FurniData[] furniRemoved;

    /** The furniture added to the scene (or null for none). */
    public FurniData[] furniAdded;

    /**
     * Initialize the update iwth all necessary data.
     */
    public void initialize (
            int targetId, int targetVersion,
            FurniData[] removed, FurniData[] added)
    {
        init(targetId, targetVersion);

        furniRemoved = removed;
        furniAdded = added;
    }

    @Override
    public void apply (SceneModel model)
    {
        super.apply(model);

        // cast it to our model type
        MsoySceneModel mmodel = (MsoySceneModel) model;

        // remove old furni, add the new
        if (furniRemoved != null) {
            for (FurniData furn : furniRemoved) {
                mmodel.removeFurni(furn);
            }
        }
        if (furniAdded != null) {
            for (FurniData furn : furniAdded) {
                mmodel.addFurni(furn);
            }
        }
    }
}
