//
// $Id: MemberLocal.java 19594 2010-11-19 16:47:28Z zell $

package com.threerings.msoy.server;

import com.threerings.presents.server.ClientLocal;

/**
 * Contains server-side only information for a member client.
 */
public class MemberClientLocal extends ClientLocal
{
    /** The member ids that this user has muted in previous sessions. May be null.
     * Note: Only valid between client resolution and sending the bootstrap,
     * and is always null otherwise and never transmitted between nodes. */
    public transient int[] mutedMemberIds;
}
