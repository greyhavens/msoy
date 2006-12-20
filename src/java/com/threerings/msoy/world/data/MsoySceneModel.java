//
// $Id$

package com.threerings.msoy.world.data;

import java.util.Iterator;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.ListUtil;

import com.threerings.whirled.data.SceneModel;

import com.threerings.whirled.spot.data.Portal;

/**
 * Extends basic scene model with scene type.
 */
public class MsoySceneModel extends SceneModel
{
    /** A type constant indicating a normal room where defaultly
     * draw some walls. */
    public static final byte DRAWN_ROOM = 0;

    /** A type constant indicating a room where the background image should
     * be drawn covering everything, but layered behind everything else such
     * that the background image IS the scene to the viewer. */
    public static final byte IMAGE_OVERLAY = 1;

    /** The number of type constants. */
    public static final int TYPE_COUNT = 2;

    /** Constant for Member room owners **/
    public static final byte OWNER_TYPE_MEMBER = 1;

    /** Constant for Group room owners **/
    public static final byte OWNER_TYPE_GROUP = 2;

    /** The type of owner that owns this scene. */
    public byte ownerType;

    /** The id of the owner of this scene, interpreted using ownerType. */
    public int ownerId;

    /** The type of scene that this is. Determines how it is rendered. */
    public byte sceneType;

    /** The "pixel" depth of the room. */
    public short depth;

    /** The pixel width of the room. */
    public short width;

    /** A value between 0 - 1, for the height of the horizon in the room. */
    public float horizon;

    /** The furniture in the scene. */
    public FurniData[] furnis = new FurniData[0];

    /** The entrance location. */
    public MsoyLocation entrance;

    /**
     * Add a piece of furniture to this model.
     */
    public void addFurni (FurniData furni)
    {
        furnis = (FurniData[]) ArrayUtil.append(furnis, furni);
        invalidatePortalInfo(furni);
    }

    /**
     * Remove a piece of furniture from this model.
     */
    public void removeFurni (FurniData furni)
    {
        int idx = ListUtil.indexOf(furnis, furni);
        if (idx != -1) {
            furnis = (FurniData[]) ArrayUtil.splice(furnis, idx, 1);
            invalidatePortalInfo(furni);
        }
    }

    /**
     * Get the next available furni id.
     */
    public short getNextFurniId (short aboveId)
    {
        int length = (furnis == null) ? 0 : furnis.length;
        for (int ii=aboveId + 1; ii != aboveId; ii++) {
            if (ii > Short.MAX_VALUE) {
                ii = Short.MIN_VALUE;
                if (ii == aboveId) {
                    break;
                }
            }
            boolean found = false;
            for (int idx=0; idx < length; idx++) {
                if (furnis[idx].id == ii) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return (short) ii;
            }
        }
        return (short) -1;
    }

    /**
     * Support for SpotScene.
     */
    public Portal getDefaultEntrance ()
    {
        Portal p = new Portal();
        p.portalId = (short) -1;
        p.loc = entrance;
        p.targetSceneId = sceneId;
        p.targetPortalId = -1;

        return p;
    }

    /**
     * Support for SpotScene.
     */
    public Portal getPortal (int portalId)
    {
        validatePortalInfo();
        return _portalInfo.get(portalId);
    }

    /**
     * Support for SpotScene.
     */
    public int getPortalCount ()
    {
        validatePortalInfo();
        return _portalInfo.size();
    }

    /**
     * Support for SpotScene.
     */
    public Iterator getPortals ()
    {
        validatePortalInfo();
        return _portalInfo.values().iterator();
    }

    /**
     * Invalidate our portal info if the specified piece of furniture
     * is a portal.
     */
    protected void invalidatePortalInfo (FurniData changedFurni)
    {
        if (changedFurni.actionType == FurniData.ACTION_PORTAL) {
            invalidatePortalInfo();
        }
    }

    /**
     * Invalidate our cached portal info.
     */
    protected void invalidatePortalInfo ()
    {
        _portalInfo = null;
    }
    
    /**
     * Validate that the portalInfo is up-to-date and ready to use.
     */
    protected void validatePortalInfo ()
    {
        // if non-null, we're already valid
        if (_portalInfo != null) {
            return;
        }

        _portalInfo = new HashIntMap<Portal>();
        for (FurniData furni : furnis) {
            if (furni.actionType != FurniData.ACTION_PORTAL) {
                continue;
            }

            String[] vals = furni.actionData.split(":");

            Portal p = new Portal();
            p.portalId = furni.id;
            p.loc = furni.loc;
            try {
                p.targetSceneId = Integer.parseInt(vals[0]);
            } catch (Exception e) {
                // TODO: eventually, all data from the clients will
                // have to be extensively verified
            }
            p.targetPortalId = (short) -1;
//            try {
//                p.targetPortalId = (short) Integer.parseInt(vals[1]);
//            } catch (Exception e) {
//                // same as above
//            }

            // remember this portal
            _portalInfo.put(p.portalId, p);
        }
    }

    public Object clone ()
        throws CloneNotSupportedException
    {
        MsoySceneModel model = (MsoySceneModel) super.clone();
        model.furnis = furnis.clone();
        model.invalidatePortalInfo();
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
        model.entrance = new MsoyLocation(.5, 0, .5, 180);
        populateBlankMsoySceneModel(model);
        return model;
    }

    protected static void populateBlankMsoySceneModel (MsoySceneModel model)
    {
        populateBlankSceneModel(model);
    }

    /** Cached portal info. */
    protected transient HashIntMap<Portal> _portalInfo;
}
