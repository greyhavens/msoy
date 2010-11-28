//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.data.BodyObject;

/**
 * Contains additional information for a body in Whirled.
 */
public interface MsoyBodyObject
{
    BodyObject body ();

    boolean isActor ();

    String getActorState ();

    public void setActorState (String value);
}
