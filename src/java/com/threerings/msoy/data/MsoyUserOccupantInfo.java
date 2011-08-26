//
// $Id$

package com.threerings.msoy.data;

@com.threerings.util.ActionScript(omit=true)
public interface MsoyUserOccupantInfo
{
    /**
     * Is this user a subscriber?
     */
    boolean isSubscriber ();

    /**
     * Update things based on the tokens. Return true if there were changes.
     */
    boolean updateTokens (MsoyTokenRing tokens);
}
