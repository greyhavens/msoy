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

    /** The memberId of the owner of this scene. */
    public int ownerId;

    /** The pixel width of the room. */
    public short width;

    /** The background image of the scene. */
    public MediaData background;

    /** The background music for the scene. */
    public MediaData music;

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

    /**
     * Create a blank scene.
     */
    public static MsoySceneModel blankMsoySceneModel ()
    {
        MsoySceneModel model = new MsoySceneModel();
        model.width = 800;
        populateBlankMsoySceneModel(model);
        return model;
    }

    protected static void populateBlankMsoySceneModel (MsoySceneModel model)
    {
        populateBlankSceneModel(model);
        model.addAuxModel(new SpotSceneModel());
    }
}
