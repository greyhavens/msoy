//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.util.Name;

import com.threerings.parlor.game.data.UserIdentifier;

import com.threerings.msoy.data.all.MemberName;

@com.threerings.util.ActionScript(omit=true)
public class MsoyUserIdentifier
    implements UserIdentifier.Ider
{
    // from UserIdentifier.Ider
    public int getUserId (Name name)
    {
        return ((MemberName) name).getId();
    }
}
