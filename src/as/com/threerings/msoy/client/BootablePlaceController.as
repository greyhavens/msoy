//
// $Id$

package com.threerings.msoy.client {

public interface BootablePlaceController
{
    /**
     * Can the local user boot people from this place?
     */
    function canBoot () :Boolean;
}
}
