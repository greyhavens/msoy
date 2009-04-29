//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.util.Name;

import com.threerings.parlor.game.data.UserIdentifier;

import com.threerings.msoy.data.all.MemberName;

public class MsoyUserIdentifier
    implements UserIdentifier
{
    public static final MsoyUserIdentifier SINGLETON = new MsoyUserIdentifier();

    // from UserIdentifier
    public int getUserId (Name name)
    {
        return ((MemberName) name).getMemberId();
    }
}
