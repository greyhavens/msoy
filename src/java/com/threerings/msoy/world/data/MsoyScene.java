//
// $Id$

package com.threerings.msoy.world.data;

import java.util.Iterator;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneImpl;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SpotScene;
import com.threerings.whirled.spot.data.SpotSceneImpl;
import com.threerings.whirled.spot.data.SpotSceneModel;

import com.threerings.msoy.data.MediaData;

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
     * Returns the type of the scene.
     */
    public String getType ()
    {
        return _model.type;
    }

    /**
     * Returns the pixel width of the scene.
     */
    public short getWidth ()
    {
        return _model.width;
    }

    /**
     * Get the background image.
     */
    public MediaData getBackground ()
    {
        return _model.background;
    }

    /**
     * Get the background music.
     */
    public MediaData getMusic ()
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
