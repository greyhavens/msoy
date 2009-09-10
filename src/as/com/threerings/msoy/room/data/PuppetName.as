//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.msoy.data.all.MemberName;

/**
 * A marker class that indicates that the user is not real, it's a puppet.
 */
public class PuppetName extends MemberName
{
    /** Deserialization. */
    public function PuppetName ()
    {
    }
}
}
