//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.presents.dobj.DObject;

/**
 * Used on the server to listen to subscriber count changes to a lobby object.
 */
public interface SubscriberListener
{
    /**
     * The number of subscribers has changed.
     */
    public void subscriberCountChanged (DObject target);
}
