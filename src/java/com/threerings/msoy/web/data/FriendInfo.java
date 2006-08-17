//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a friend connection.
 */
public class FriendInfo
    implements IsSerializable
{
    /** Out friend's member id. */
    public int memberId;

    /** Our friend's display name. */
    public String name;

    /** The status of this friend connection. */
    public byte status;
}
