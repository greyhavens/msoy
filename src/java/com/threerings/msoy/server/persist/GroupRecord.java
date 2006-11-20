//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;

import com.samskivert.util.StringUtil;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.web.data.Group;

/**
 * Contains the details of a group.
 */
@Entity
public class GroupRecord
    implements Cloneable
{
    public static final int SCHEMA_VERSION = 1;

    public static final String GROUP_ID = "groupId";
    public static final String NAME = "name";
    public static final String CHARTER = "charter";
    public static final String LOGO_MIME_TYPE = "logoMimeType";
    public static final String LOGO_MEDIA_HASH = "logoMediaHash";
    public static final String CREATOR_ID = "creatorId";
    public static final String CREATION_DATE = "creationDate";
    public static final String POLICY = "policy";

    /** The unique id of this group. */
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int groupId;

    /** The name of the group. */
    @Column(unique=true)
    public String name;

    /** The group's charter, or null if one has yet to be set. */
    @Column(length=2048, nullable=true)
    public String charter;
    
    /** The MIME type of this group's logo. */
    public byte logoMimeType;

    /** A hash code identifying the media for this group's logo. */
    @Column(nullable=true)
    public byte[] logoMediaHash;

    /** The member id of the person who created the group. */
    public int creatorId;
    
    /** The date and time this group was created. */
    public Timestamp creationDate;

    /** The group may be public, invite-only or exclusive as per {@link Group}. */
    public byte policy;
    
    /**
     * CreateS a web-safe version of this group.
     */
    public Group toWebObject ()
    {
        Group group = new Group();
        group.groupId = groupId;
        group.name = name;
        group.charter = charter;
        group.logo = logoMediaHash != null ?
            new MediaDesc(logoMediaHash.clone(), logoMimeType) : null;
        group.creatorId = creatorId;
        group.creationDate = new Date(creationDate.getTime());
        group.policy = policy;
        return group;
    }

    /**
     * Generates a string representation of this instance.
     */
    @Override
    public String toString ()
    {
        StringBuilder buf = new StringBuilder("[");
        StringUtil.fieldsToString(buf, this);
        return buf.append("]").toString();
    }

}
