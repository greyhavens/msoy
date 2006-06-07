//
// $Id$

package com.threerings.msoy.world.data;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ListUtil;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.spot.data.SpotSceneModel;

import com.threerings.msoy.data.MediaData;

/**
 * Extends basic scene model with scene type.
 */
public class MsoySceneModel extends SceneModel
{
    /** The type of scene that this is. Determines how it is rendered. */
    public String type;

    /** The background image of the scene. */
    public MediaData background;

    /** The furniture in the scene. */
    public FurniData[] furnis = new FurniData[0];

    /**
     * Add a piece of furniture to this model.
     */
    public void addFurni (FurniData furni)
    {
        furnis = (FurniData[]) ArrayUtil.append(furnis, furni);
    }

    /**
     * Remove a piece of furniture from this model.
     */
    public void removeFurni (FurniData furni)
    {
        int idx = ListUtil.indexOf(furnis, furni);
        if (idx != -1) {
            furnis = (FurniData[]) ArrayUtil.splice(furnis, idx, 1);
        }
    }

    /**
     * Create a blank scene.
     */
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
    }
}
