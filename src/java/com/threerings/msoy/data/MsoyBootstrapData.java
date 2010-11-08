//
// $Id$

package com.threerings.msoy.data;

import com.threerings.presents.net.BootstrapData;

/**
 * Msoy bootstrap data.
 */
public class MsoyBootstrapData extends BootstrapData
{
    /** An array of memberIds that we've muted in previous sessions. */
    public int[] mutedMemberIds;

    /** The signed URL of our MediaStub.swf */ 
    public String stubUrl;
}
