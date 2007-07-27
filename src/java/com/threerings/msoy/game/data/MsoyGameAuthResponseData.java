//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.presents.net.AuthResponseData;

/**
 * Extends the normal auth response data with MSOY Game-specific bits.
 */
public class MsoyGameAuthResponseData extends AuthResponseData
{
    /** Contains the oid of the game lobby on this server. */
    public int lobbyOid;
}
