//
// $Id$

package com.threerings.msoy.data {

import com.threerings.util.Name;

/**
 * Implemented by PlaceObjects that take over the PlaceView completely. (ie, not AVRGs)
 */
public interface PrimaryPlace
{
    /**
     * Get the name of this place.
     */
    function getName () :Name;
}
}
