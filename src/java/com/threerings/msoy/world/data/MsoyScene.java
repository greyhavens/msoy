//
// $Id$

package com.threerings.msoy.world.data;

import java.util.Iterator;

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
public class MsoyScene extends SceneImpl
    implements Scene, SpotScene, Cloneable
{
    public MsoyScene (MsoySceneModel model, PlaceConfig config)
    {
        super(model, config);
        _model = model;
    }

    /**
     * Can the specified member edit this scene?
     */
    public boolean canEdit (MemberObject member)
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

        if (!hasRights && member.tokens.isSupport()) {
            log.info("Allowing support+ to edit scene in which they otherwise wouldn't have " +
                     "rights [sceneId=" + getId() + ", sceneName=\"" + getName() + "\", " +
                     "support=" + member.who() + "].");
            return true;
        }

        return hasRights;
    }

    /**
     * Can the specified member enter the scene?
     */
    public boolean canEnter (MemberObject member)
    {
        boolean hasRights = _model.canEnter(member);

        if (! hasRights && member.tokens.isSupport()) {
            log.info("Allowing support+ to enter scene which they otherwise couldn't enter " +
                     "[sceneId=" + getId() + ", sceneName=\"" + getName() +
                     "\", accessControl=" + _model.accessControl + "].");
            return true;
        }

        return hasRights;
    }

    /**
     * Returns the access control for the scene. 
     */
    public byte getAccessControl ()
    {
        return _model.accessControl;
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
     * Retrieve the scene background audio.
     */
    public AudioData getAudioData ()
    {
        return _model.audioData;
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
    public Iterator getPortals ()
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
    public Object clone ()
        throws CloneNotSupportedException
    {
        return new MsoyScene((MsoySceneModel) _model.clone(), _config);
    }

    /** A reference to our scene model. */
    protected MsoySceneModel _model;
}
