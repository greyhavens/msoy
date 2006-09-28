//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A GTW-safe class corresponding to {@link MemberName}.
 */

public class MemberGName 
    implements IsSerializable
{
    /** This member's name. */
    public String memberName;
    
    /** This member's ID. */
    public int memberId;

    /** Constructor for unserializing. */
    public MemberGName ()
    {
        super();
    }
    
    /** Constructor for configuration. */
    public MemberGName (String name, int id)
    {
        super();
        this.memberName = name;
        this.memberId = id;
    }
}
