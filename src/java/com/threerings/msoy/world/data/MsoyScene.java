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

    public void addFurni (FurniData furn)
    {
        // TODO
    }

    public void removeFuni (FurniData furn)
    {
        // TODO
    }

    // documentation inherited from interface SpotScene
    public void addPortal (Portal portal)
    {
        _sdelegate.addPortal(portal);
    }
    
    // documentation inherited from interface SpotScene
    public Portal getDefaultEntrance ()
    {
        return _sdelegate.getDefaultEntrance();
    }
    
    // documentation inherited from interface SpotScene
    public short getNextPortalId ()
    {
        return _sdelegate.getNextPortalId();
    }

    // documentation inherited from interface SpotScene
    public Portal getPortal (int portalId)
    {
        return _sdelegate.getPortal(portalId);
    }
    
    // documentation inherited from interface SpotScene
    public int getPortalCount ()
    {
        return _sdelegate.getPortalCount();
    }
    
    // documentation inherited from interface SpotScene
    public Iterator getPortals ()
    {
        return _sdelegate.getPortals();
    }

    // documentation inherited from interface SpotScene
    public void removePortal (Portal portal)
    {
        _sdelegate.removePortal(portal);
    }

    // documentation inherited from interface SpotScene
    public void setDefaultEntrance (Portal portal)
    {
        _sdelegate.setDefaultEntrance(portal);
    }

    // documentation inherited from interface Cloneable
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
