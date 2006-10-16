//
// $Id$

package com.threerings.msoy.web.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Contains the details of a group.
 */
public class Group
    implements Streamable, IsSerializable
{
    public static final byte POLICY_PUBLIC = 1;
    public static final byte POLICY_INVITE_ONLY = 2;
    public static final byte POLICY_EXCLUSIVE = 3;

    /** The unique id of this group. */
    public int groupId;

    /** The name of the group. */
    public String name;

    /** The group's charter, or null if one has yet to be set. */
    public String charter;

    /** The MIME type of this group's logo. */
    public byte logoMimeType;

    /** A hash code identifying the media for this group's logo. */
    public byte[] logoMediaHash;

    /** The member id of the person who created the group. */
    public int creatorId;
    
    public Date creationDate;

    public byte policy;
}
