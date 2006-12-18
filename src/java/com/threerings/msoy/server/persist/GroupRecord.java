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
    public static final int SCHEMA_VERSION = 5;

    public static final String GROUP_ID = "groupId";
    public static final String NAME = "name";
    public static final String HOMEPAGE_URL = "homepageUrl";
    public static final String BLURB = "blurb";
    public static final String CHARTER = "charter";
    public static final String LOGO_MIME_TYPE = "logoMimeType";
    public static final String LOGO_MEDIA_HASH = "logoMediaHash";
    public static final String INFO_BACKGROUND_MIME_TYPE = "infoBackgroundMimeType";
    public static final String INFO_BACKGROUND_HASH = "infoBackgroundHash";
    public static final String DETAIL_BACKGROUND_MIME_TYPE = "detailBackgroundMimeType";
    public static final String DETAIL_BACKGROUND_HASH = "detailBackgroundHash";
    public static final String PEOPLE_BACKGROUND_MIME_TYPE = "peopleBackgroundMimeType";
    public static final String PEOPLE_BACKGROUND_HASH = "peopleBackgroundHash";
    public static final String CREATOR_ID = "creatorId";
    public static final String HOME_SCENE_ID = "homeSceneId";
    public static final String CREATION_DATE = "creationDate";
    public static final String POLICY = "policy";

    /** The unique id of this group. */
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int groupId;

    /** The name of the group. */
    @Column(unique=true)
    public String name;

    /** The URL for the grou's homepage. */
    @Column(nullable=true)
    public String homepageUrl;

    /** The blurb for the group. */
    @Column(length=80, nullable=true)
    public String blurb;

    /** The group's charter, or null if one has yet to be set. */
    @Column(length=2048, nullable=true)
    public String charter;

    /** The MIME type of this group's logo. */
    public byte logoMimeType;

    /** A hash code identifying the media for this group's logo. */
    @Column(nullable=true)
    public byte[] logoMediaHash;

    /** The MIME type for the background of the info area. */
    public byte infoBackgroundMimeType;

    /** A hash code identifying the media for the background of the info area. */
    @Column(nullable=true)
    public byte[] infoBackgroundHash;

    /** The MIME type for the background of the detail area. */
    public byte detailBackgroundMimeType;

    /** A hash code identifying the media for the background of the detail area. */
    @Column(nullable=true)
    public byte[] detailBackgroundHash;

    /** The MIME type for the background of the people area. */
    public byte peopleBackgroundMimeType;

    /** A hash code identifying the media for the background of the people area. */
    @Column(nullable=true)
    public byte[] peopleBackgroundHash;

    /** The member id of the person who created the group. */
    public int creatorId;

    /** The home scene of this group. */
    public int homeSceneId;

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
        group.homepageUrl = homepageUrl;
        group.blurb = blurb;
        group.charter = charter;
        group.logo = logoMediaHash == null ? Group.getDefaultGroupLogoMedia() :
            new MediaDesc(logoMediaHash.clone(), logoMimeType);
        // with null backgrounds, there is not default image - just don't try to tile anything
        group.infoBackground = infoBackgroundHash == null ? null :
            new MediaDesc(infoBackgroundHash.clone(), infoBackgroundMimeType);
        group.detailBackground = detailBackgroundHash == null ? null :
            new MediaDesc(detailBackgroundHash, detailBackgroundMimeType);
        group.peopleBackground = peopleBackgroundHash == null ? null :
            new MediaDesc(peopleBackgroundHash, peopleBackgroundMimeType);
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
