//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.whirled.data.AuxModel;
import com.threerings.whirled.data.SceneModel;

/**
 * Much TODO about nothing.
 */
public class MsoyFurniSceneModel extends SimpleStreamableObject
    implements AuxModel
{
    /** An array of data about furniture in the scene. */
    public FurniData[] furniData;

    // documentation inherited from interface Cloneable
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            return cnse;
        }
    }

    /**
     * Locates and returns the {@link MsoyFurniSceneModel} among the
     * auxiliary scene models associated with the supplied scene model.
     * <code>null</code> is returned if no msoy scene model could be
     * found.
     */
    public static MsoyFurniSceneModel getSceneModel (SceneModel model)
    {
        AuxModel[] mods = model.auxModels;
        for (int ii=0; ii < mods.length; ii++) {
            if (mods[ii] instanceof MsoyFurniSceneModel) {
                return (MsoyFurniSceneModel) mods[ii];
            }
        }
        return null;
    }
}
