//
// $Id$

package com.threerings.msoy.server;

import com.threerings.presents.data.ClientObject;

import com.threerings.msoy.data.MsoyUserObject;

import com.threerings.crowd.data.TokenRing;
import com.threerings.crowd.server.CrowdClientResolver;

/**
 * Used to configure msoy-specific client object data.
 */
public class MsoyClientResolver extends CrowdClientResolver
{
    @Override // documentation inherited
    public Class getClientObjectClass ()
    {
        return MsoyUserObject.class;
    }

    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);

        MsoyUserObject userObj = (MsoyUserObject) clobj;
        userObj.setTokens(new TokenRing());
    }
}
