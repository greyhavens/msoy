//
// $Id$

package com.threerings.msoy.server;

import com.threerings.msoy.data.MemberObject;

/**
 * A place that allows people to boot others.
 */
public interface BootablePlaceManager
{
    /**
     * Attempt to boot the specified user from this place.
     * Return a translatable error String or null for success.
     */
    String bootFromPlace (MemberObject booter, int booteeId);
}
