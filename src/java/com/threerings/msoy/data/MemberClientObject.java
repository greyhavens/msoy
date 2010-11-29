//
// $Id: $


package com.threerings.msoy.data;

import com.threerings.presents.data.ClientObject;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.AuthenticationDomain.Account;

/**
 * The tiny loader client for a member.
 */
public class MemberClientObject extends ClientObject
{
    /** The name of the member we're loading for. */
    public MemberName name;

    /** The oid of the {@link MemberObject} once it's loaded; zero until then. */
    public int bodyOid;
}
