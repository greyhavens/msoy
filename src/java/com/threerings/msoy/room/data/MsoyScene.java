//
// $Id$

package com.threerings.msoy.room.data;

import java.util.Iterator;

import com.threerings.util.Name;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneImpl;
import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SpotScene;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.item.data.all.Decor;

import static com.threerings.msoy.Log.log;

/**
 * Implementation of the Msoy scene interface.
 */
@com.threerings.util.ActionScript(omit=true)
public class MsoyScene extends SceneImpl
    implements Scene, SpotScene, Cloneable
{
    public MsoyScene (MsoySceneModel model, PlaceConfig config)
    {
        super(model, config);
        _model = model;
    }

    /**
     * Does the specified member have management rights in this room?
     * Don't log anything when support staff is given access.
     */
    public boolean canManage (MemberObject member)
    {
        return canManage(member, true, null);
    }

    /**
     * Does the specified member have management rights in this room?
     */
    public boolean canManage (MemberObject member, boolean allowSupport, String where)
    {
        boolean hasRights;
        switch (_model.ownerType) {
        case MsoySceneModel.OWNER_TYPE_MEMBER:
            hasRights = (_model.ownerId == member.getMemberId());
            break;

        case MsoySceneModel.OWNER_TYPE_GROUP:
            hasRights = member.isGroupManager(_model.ownerId);
            break;

        default:
            hasRights = false;
            break;
        }

        if (!hasRights && allowSupport && member.tokens.isSupport()) {
            if (where != null) {
                log.info("Allowing support+ to manage scene in which they don't have rights",
                    "where", where, "sceneId", getId(), "sceneName", getName(),
                    "support", member.who());
            }
            return true;
        }

        return hasRights;
    }

    /**
     * Returns the whirled id for this scene, or zero.
     */
    public int getThemeId ()
    {
        return _model.themeId;
    }

    /**
     * Returns the owner id for the scene.
     */
    public int getOwnerId ()
    {
        return _model.ownerId;
    }

    /**
     * Returns the name of the owner of this scene (MemberName or GroupName).
     */
    public Name getOwner ()
    {
        return _model.ownerName;
    }

    /**
     * Returns the owner type for the scene.
     */
    public byte getOwnerType ()
    {
        return _model.ownerType;
    }

    /**
     * Returns the access control for the scene.
     */
    public byte getAccessControl ()
    {
        return _model.accessControl;
    }

    /**
     * Returns the playlist control for the scene.
     */
    public byte getPlaylistControl ()
    {
        return _model.playlistControl;
    }

    /**
     * Returns the type of the scene.
     */
    public byte getSceneType ()
    {
        return _model.decor.type;
    }

    /**
     * Returns the "pixel" depth of the scene.
     */
    public short getDepth ()
    {
        return _model.decor.depth;
    }

    /**
     * Returns the pixel width of the scene.
     */
    public short getWidth ()
    {
        return _model.decor.width;
    }

    /**
     * Get the height of the horizon, expressed as a floating
     * point number between 0 and 1. (1 == horizon at top of screen)
     */
    public float getHorizon ()
    {
        return _model.decor.horizon;
    }

    /**
     * Retrieve the room entrance.
     */
    public MsoyLocation getEntrance ()
    {
        return _model.entrance;
    }

    /**
     * Retrieve the room decor.
     */
    public Decor getDecor ()
    {
        return _model.decor;
    }

    /**
     * Add the specified furniture to the scene.
     */
    public void addFurni (FurniData furn)
    {
        _model.addFurni(furn);
    }

    /**
     * Remove the specified furniture from the scene.
     */
    public void removeFuni (FurniData furn)
    {
        _model.removeFurni(furn);
    }

    /**
     * Get all the furniture currently in the scene.
     */
    public FurniData[] getFurni ()
    {
        return _model.furnis;
    }

    /**
     * Get the next available furni id.
     */
    public short getNextFurniId (short aboveId)
    {
        return _model.getNextFurniId(aboveId);
    }

    // from SpotScene
    public void addPortal (Portal portal)
    {
        throw new UnsupportedOperationException();
    }

    // from SpotScene
    public Portal getDefaultEntrance ()
    {
        return _model.getDefaultEntrance();
    }

    // from SpotScene
    public short getNextPortalId ()
    {
        throw new UnsupportedOperationException();
    }

    // from SpotScene
    public Portal getPortal (int portalId)
    {
        return _model.getPortal(portalId);
    }

    // from SpotScene
    public int getPortalCount ()
    {
        return _model.getPortalCount();
    }

    // from SpotScene
    public Iterator<Portal> getPortals ()
    {
        return _model.getPortals();
    }

    // from SpotScene
    public void removePortal (Portal portal)
    {
        throw new UnsupportedOperationException();
    }

    // from SpotScene
    public void setDefaultEntrance (Portal portal)
    {
        throw new UnsupportedOperationException();
    }

    // from Cloneable
    public MsoyScene clone ()
        throws CloneNotSupportedException
    {
        return new MsoyScene(_model.clone(), _config);
    }

    /** A reference to our scene model. */
    protected MsoySceneModel _model;
}
