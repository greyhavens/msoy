//
// $Id$

package com.threerings.msoy.world.data;

import java.util.Iterator;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.ListUtil;

import com.threerings.whirled.data.SceneModel;

import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.item.web.MediaDesc;

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

    /** The type of scene that this is. Determines how it is rendered. */
    public byte type;

    /** The memberId of the owner of this scene. */
    public int ownerId;

    /** The default entrance for this scene. */
    public short defaultEntranceId;

    /** The "pixel" depth of the room. */
    public short depth;

    /** The pixel width of the room. */
    public short width;

    /** A value between 0 - 1, for the height of the horizon in the room. */
    public float horizon;

    /** The furniture in the scene. */
    public FurniData[] furnis = new FurniData[0];

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
    public short getNextFurniId ()
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
        // Note that we can't just call getPortal(_defaultPortalId) because
        // we have to validate prior to accessing _defaultPortalId.
        validatePortalInfo();
        return _portalInfo.get(_defaultPortalId);
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
        if (_portalInfo != null) {
            return;
        }

        _portalInfo = new HashIntMap<Portal>();
        _defaultPortalId = Integer.MIN_VALUE;
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
            try {
                p.targetPortalId = (short) Integer.parseInt(vals[1]);
            } catch (Exception e) {
                // same as above
            }

            // TODO: something real here.. :)
            if (_defaultPortalId == Integer.MIN_VALUE) {
                _defaultPortalId = p.portalId;
            }

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
        populateBlankMsoySceneModel(model);
        return model;
    }

    protected static void populateBlankMsoySceneModel (MsoySceneModel model)
    {
        populateBlankSceneModel(model);
    }

    /** Cached portal info. */
    protected transient HashIntMap<Portal> _portalInfo;

    /** The default portal id. */
    protected transient int _defaultPortalId;
}
