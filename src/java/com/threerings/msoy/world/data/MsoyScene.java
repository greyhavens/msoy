//
// $Id$

package com.threerings.msoy.world.data;

import java.util.Iterator;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.msoy.data.MemberObject;

import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneImpl;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SpotScene;
import com.threerings.whirled.spot.data.SpotSceneImpl;
import com.threerings.whirled.spot.data.SpotSceneModel;

import com.threerings.msoy.item.web.MediaDesc;

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
        _sdelegate = new SpotSceneImpl(SpotSceneModel.getSceneModel(_model));
    }

    /**
     * Can the specified member edit this scene?
     */
    public boolean canEdit (MemberObject member)
    {
        return member.getTokens().isAdmin() ||
            (!member.isGuest() && (getOwnerId() == member.getMemberId()));
    }

    /**
     * Returns the type of the scene.
     */
    public byte getType ()
    {
        return _model.type;
    }

    /**
     * Get the memberId of the owner of this scene.
     */
    public int getOwnerId ()
    {
        return _model.ownerId;
    }

    /**
     * Returns the "pixel" depth of the scene.
     */
    public short getDepth ()
    {
        return _model.depth;
    }

    /**
     * Returns the pixel width of the scene.
     */
    public short getWidth ()
    {
        return _model.width;
    }

    /**
     * Get the height of the horizon, expressed as a floating
     * point number between 0 and 1. (1 == horizon at top of screen)
     */
    public float getHorizon ()
    {
        return _model.horizon;
    }

    /**
     * Get the background image.
     */
    public MediaDesc getBackground ()
    {
        return _model.background;
    }

    /**
     * Get the background music.
     */
    public MediaDesc getMusic ()
    {
        return _model.music;
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
    public int getNextFurniId ()
    {
        return _model.getNextFurniId();
    }

    // from SpotScene
    public void addPortal (Portal portal)
    {
        _sdelegate.addPortal(portal);
    }
    
    // from SpotScene
    public Portal getDefaultEntrance ()
    {
        return _sdelegate.getDefaultEntrance();
    }
    
    // from SpotScene
    public short getNextPortalId ()
    {
        return _sdelegate.getNextPortalId();
    }

    // from SpotScene
    public Portal getPortal (int portalId)
    {
        return _sdelegate.getPortal(portalId);
    }
    
    // from SpotScene
    public int getPortalCount ()
    {
        return _sdelegate.getPortalCount();
    }
    
    // from SpotScene
    public Iterator getPortals ()
    {
        return _sdelegate.getPortals();
    }

    // from SpotScene
    public void removePortal (Portal portal)
    {
        _sdelegate.removePortal(portal);
    }

    // from SpotScene
    public void setDefaultEntrance (Portal portal)
    {
        _sdelegate.setDefaultEntrance(portal);
    }

    @Override
    public void updateReceived (SceneUpdate update)
    {
        super.updateReceived(update);

        // inform our spot delegate of possible changes
        _sdelegate.updateReceived();
    }

    // from Cloneable
    public Object clone ()
        throws CloneNotSupportedException
    {
        return new MsoyScene((MsoySceneModel) _model.clone(), _config);
    }

    /** A reference to our scene model. */
    protected MsoySceneModel _model;

    /** Our spot scene delegate. */
    protected SpotSceneImpl _sdelegate;
}
