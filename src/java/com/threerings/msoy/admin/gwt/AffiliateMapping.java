//
// $Id$

package com.threerings.msoy.admin.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Displays the mapping from an affiliate to a memberId.
 */
public class AffiliateMapping
    implements IsSerializable
{
    /** The affiliate tag, as noted during registration time. */
    public String affiliate;

    /** The associated memberId, or 0. */
    public int memberId;

    /** Suitable for unserialization. */
    public AffiliateMapping ()
    {
    }

    /** Simple constructor. */
    public AffiliateMapping (String affiliate, int memberId)
    {
        this.affiliate = affiliate;
        this.memberId = memberId;
    }
}
