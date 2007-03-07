//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Common members between Neighbourhood Groups and Members.
  */
public class NeighborEntity
    implements IsSerializable, Streamable, Cloneable
{
    /** This entity's home scene ID. */
    public int homeSceneId;

    /** How many people are currently logged into this entity's home scene. */
    public int population;
    
}
