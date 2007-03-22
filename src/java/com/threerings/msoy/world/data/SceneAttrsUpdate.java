//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;

/**
 * Encodes a scene update that updates the attributes in the MsoySceneModel.
 * Note that this contains all attributes, even ones that have not changed.
 * In other words, a field being null doesn't mean that the field
 * isn't updated, it means the new value should be null.
 */
public class SceneAttrsUpdate extends SceneUpdate
{
    /** The new name. */
    public String name;

    /** Full description of the new decor. */
    public DecorData decorData;
    
    /** The new entrance location. */
    public MsoyLocation entrance;

    @Override
    public void apply (SceneModel model)
    {
        super.apply(model);

        MsoySceneModel mmodel = (MsoySceneModel) model;
        mmodel.name = name;
        mmodel.decorData = (DecorData) decorData.clone();
        mmodel.entrance = entrance;
    }

    @Override
    public void validate (SceneModel model)
        throws IllegalStateException
    {
        super.validate(model);
        // FIXME ROBERT: we should validate this update's decor data here, on the server side
    }

}
