//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * DTO containing information about a charity.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class CharityInfo
    implements IsSerializable
{
    /** Member ID of the charity. */
    public /* final */ int memberId;

    /** Whether or not the charity can be randomly selected by members. */
    public /* final */ boolean core;

    /** Description of the charity available to members. */
    public /* final */ String description;

    public CharityInfo (int memberId, boolean core, String description)
    {
        super();
        this.memberId = memberId;
        this.core = core;
        this.description = description;
    }

    public CharityInfo () { /* For serialization */ }
}
