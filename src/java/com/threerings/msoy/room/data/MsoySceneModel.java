//
// $Id$

package com.threerings.msoy.room.data;

import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.Maps;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ListUtil;

import com.threerings.util.Name;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.spot.data.Portal;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.DefaultItemMediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MsoyItemType;

import static com.threerings.msoy.Log.log;

/**
 * Extends basic scene model with scene type.
 */
@com.threerings.util.ActionScript(omit=true)
public class MsoySceneModel extends SceneModel
{
    /** Constant for Member room owners **/
    public static final byte OWNER_TYPE_MEMBER = 1;

    /** Constant for Group room owners **/
    public static final byte OWNER_TYPE_GROUP = 2;

    /**
     * Constant for transient rooms. Rooms with this owner type are read-only, don't belong to
     * anyone, and may be destroyed at a later date.
     */
    public static final byte OWNER_TYPE_TRANSIENT = 3;

    /** Access control constant, denotes that anyone can enter this scene. */
    public static final byte ACCESS_EVERYONE = 0;

    /** Access control constant, denotes that only the scene owner and friends
     *  (or group manager and members, in case of a group scene) can enter this scene. */
    public static final byte ACCESS_OWNER_AND_FRIENDS = 1;

    /** Access control constant, denotes that only the scene owner (or group manager,
     *  in case of a group scene) can enter this scene. */
    public static final byte ACCESS_OWNER_ONLY = 2;

    /** The maximum length of a room name. */
    public static final int MAX_NAME_LENGTH = 80;

    /** The default decor media. */
    public static final MediaDesc DEFAULT_DECOR_MEDIA =
        new DefaultItemMediaDesc(MediaMimeTypes.IMAGE_PNG, MsoyItemType.DECOR, Item.MAIN_MEDIA);

    /** Access control, as one of the ACCESS constants. Limits who can enter the scene. */
    public byte accessControl;

    /** Uses the accessControl constants to control who can add to the playlist. */
    public byte playlistControl;

    /** The type of owner that owns this scene. */
    public byte ownerType;

    /** The id of the owner of this scene, interpreted using ownerType. */
    public int ownerId;

    /** The name of the owner, either a MemberName or GroupName. */
    public Name ownerName;

    /** The group of the Mog associated with this room, or 0 if none. */
    public int themeId;

    /** The game associated with this room (usually the group's game), or 0 if none. */
    public int gameId;

    /** The furniture in the scene. */
    public FurniData[] furnis = new FurniData[0];

    /** The entrance location. */
    public MsoyLocation entrance;

    /** Decor item reference. */
    public Decor decor;

    /** Color to use around and under the decor (by default). */
    public int backgroundColor;

    /** If the puppet is hidden for this room. */
    public boolean noPuppet;

    /** Constructor. */
    public MsoySceneModel ()
    {
    }

    /**
     * Add a piece of furniture to this model.
     */
    public void addFurni (FurniData furni)
    {
        furnis = ArrayUtil.append(furnis, furni);
        invalidatePortalInfo(furni);
    }

    /**
     * Remove a piece of furniture from this model.
     */
    public void removeFurni (FurniData furni)
    {
        int idx = ListUtil.indexOf(furnis, furni);
        if (idx != -1) {
            furnis = ArrayUtil.splice(furnis, idx, 1);
            invalidatePortalInfo(furni);
        }
    }

    /**
     * Updates a piece of furniture in this model.
     */
    public void updateFurni (FurniData furni)
    {
        int idx = ListUtil.indexOf(furnis, furni);
        if (idx != -1) {
            furnis[idx] = furni;
            invalidatePortalInfo(furni);
        } else {
            log.warning("Requested to update furni not in scene [id=" + sceneId + ", name=" + name +
                        ", furni=" + furni + "].");
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
        return _portalInfo.get(Short.valueOf((short) portalId));
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
    public Iterator<Portal> getPortals ()
    {
        validatePortalInfo();
        return _portalInfo.values().iterator();
    }

    /**
     * Invalidate our portal info if the specified piece of furniture is a portal.
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

        _portalInfo = Maps.newHashMap();
        for (FurniData furni : furnis) {
            if (furni.actionType != FurniData.ACTION_PORTAL) {
                continue;
            }
            try {
                MsoyPortal p = new MsoyPortal(furni);
                _portalInfo.put(p.portalId, p);
            } catch (Exception e) {
                log.warning("Invalid portal furni [fd=" + furni + "].", e);
            }
        }
    }

    public MsoySceneModel clone ()
        throws CloneNotSupportedException
    {
        MsoySceneModel model = (MsoySceneModel) super.clone();
        model.furnis = furnis.clone();
        model.entrance = entrance.clone();
        // decor and ownerName are ok to just copy by reference
        model.invalidatePortalInfo();
        return model;
    }

    /**
     * Create a blank scene, with default decor data.
     */
    public static MsoySceneModel blankMsoySceneModel ()
    {
        MsoySceneModel model = new MsoySceneModel();
        model.accessControl = MsoySceneModel.ACCESS_EVERYONE;
        model.entrance = new MsoyLocation(.5, 0, .5, 180);
        model.decor = defaultMsoySceneModelDecor();
        populateBlankMsoySceneModel(model);
        return model;
    }

    /**
     * Create a default decor for a blank scene. The decor will not be completely filled in,
     * because it doesn't correspond to an entity inside the database, but it has enough to be
     * displayed inside the room.
     */
    public static Decor defaultMsoySceneModelDecor ()
    {
        Decor decor = new Decor();
        decor.itemId = 0; // doesn't correspond to an object
        decor.setFurniMedia(DEFAULT_DECOR_MEDIA);
        decor.type = Decor.IMAGE_OVERLAY;
        decor.hideWalls = false;
        decor.width = 800;
        decor.height = 494;
        decor.depth = 400;
        decor.horizon = .5f;
        decor.actorScale = 1;
        decor.furniScale = 1;
        return decor;
    }

    protected static void populateBlankMsoySceneModel (MsoySceneModel model)
    {
        populateBlankSceneModel(model);
    }

    /** Cached portal info. */
    protected transient Map<Short, Portal> _portalInfo;

}
