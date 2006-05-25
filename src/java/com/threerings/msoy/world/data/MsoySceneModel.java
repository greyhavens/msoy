//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.spot.data.SpotSceneModel;

/**
 * Extends basic scene model with scene type.
 */
public class MsoySceneModel extends SceneModel
{
    /** The type of scene that this is. Determines how it is rendered. */
    public String type;

    public static MsoySceneModel blankMsoySceneModel ()
    {
        MsoySceneModel model = new MsoySceneModel();
        populateBlankMsoySceneModel(model);
        return model;
    }

    protected static void populateBlankMsoySceneModel (MsoySceneModel model)
    {
        populateBlankSceneModel(model);
        model.addAuxModel(new SpotSceneModel());
        model.addAuxModel(new MsoyFurniSceneModel());
    }
}
