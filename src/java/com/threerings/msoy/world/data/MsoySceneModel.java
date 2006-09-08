//
// $Id$

package com.threerings.msoy.world.data;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ListUtil;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.spot.data.SpotSceneModel;

import com.threerings.msoy.data.MediaDesc;

/**
 * Extends basic scene model with scene type.
 */
public class MsoySceneModel extends SceneModel
{
    /** The type of scene that this is. Determines how it is rendered. */
    public String type;

    /** The memberId of the owner of this scene. */
    public int ownerId;

    /** The "pixel" depth of the room. */
    public short depth;

    /** The pixel width of the room. */
    public short width;

    /** A value between 0 - 1, for the height of the horizon in the room. */
    public float horizon;

    /** The background image of the scene. */
    public MediaDesc background;

    /** The background music for the scene. */
    public MediaDesc music;

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
     * Get the next available furni id.
     */
    public int getNextFurniId ()
    {
        // TODO?
        int length = furnis.length;
        for (int ii=1; ii < 5000; ii++) {
            boolean found = false;
            for (int idx=0; idx < length; idx++) {
                if (furnis[idx].id == ii) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return ii;
            }
        }
        return -1;
    }

    public Object clone ()
        throws CloneNotSupportedException
    {
        MsoySceneModel model = (MsoySceneModel) super.clone();
        model.furnis = furnis.clone();
        return model;
    }

    /**
     * Create a blank scene.
     */
    public static MsoySceneModel blankMsoySceneModel ()
    {
        MsoySceneModel model = new MsoySceneModel();
        model.depth = 400;
        model.width = 800;
        model.horizon = .5f;
        populateBlankMsoySceneModel(model);
        return model;
    }

    protected static void populateBlankMsoySceneModel (MsoySceneModel model)
    {
        populateBlankSceneModel(model);
        model.addAuxModel(new SpotSceneModel());
    }
}
