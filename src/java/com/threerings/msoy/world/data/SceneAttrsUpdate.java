//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.msoy.item.web.MediaDesc;

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

    /** The new type. */
    public byte sceneType;

    /** The new depth. */
    public short depth;

    /** The new width. */
    public short width;

    /** The new horizon. */
    public float horizon;

    /** The new entrance location. */
    public MsoyLocation entrance;

    @Override
    public void apply (SceneModel model)
    {
        super.apply(model);

        MsoySceneModel mmodel = (MsoySceneModel) model;
        mmodel.name = name;
        mmodel.sceneType = sceneType;
        mmodel.depth = depth;
        mmodel.width = width;
        mmodel.horizon = horizon;
        mmodel.entrance = entrance;
    }
}
