//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.io.PersistenceException;

import com.samskivert.util.StringUtil;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupExtras;

/**
 * Contains the details of a group.
 */
@Entity(indices={
    @Index(name="searchIndex", type="fulltext", columns={"name", "blurb", "charter"})
})
public class GroupRecord
    implements Cloneable
{
    public static final int SCHEMA_VERSION = 8;

    public static final String GROUP_ID = "groupId";
    public static final String NAME = "name";
    public static final String HOMEPAGE_URL = "homepageUrl";
    public static final String BLURB = "blurb";
    public static final String CHARTER = "charter";
    public static final String LOGO_MIME_TYPE = "logoMimeType";
    public static final String LOGO_MEDIA_HASH = "logoMediaHash";
    public static final String LOGO_MEDIA_CONSTRAINT = "logoMediaConstraint";
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

    /** A hash code identifying the media for this group's logo. */
    @Column(nullable=true)
    public byte[] logoMediaHash;

    /** The MIME type of this group's logo. */
    public byte logoMimeType;

    /** The constraint for the logo image. */
    public byte logoMediaConstraint;

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

    /** The number of people that are currently members of this group. */
    public int memberCount;

    /**
     * Creates a web-safe version of this group.
     */
    public Group toGroupObject ()
    {
        Group group = new Group();
        group.groupId = groupId;
        group.name = name;
        group.blurb = blurb;
        group.logo = logoMediaHash == null ? Group.getDefaultGroupLogoMedia() :
            new MediaDesc(logoMediaHash.clone(), logoMimeType, logoMediaConstraint);
        group.creatorId = creatorId;
        group.creationDate = new Date(creationDate.getTime());
        group.policy = policy;
        group.memberCount = memberCount;
        return group;
    }

    /**
     * Creates a web-safe version of the extras in this group.
     */
    public GroupExtras toExtrasObject ()
    {
        GroupExtras extras = new GroupExtras();
        extras.infoBackground = infoBackgroundHash == null ? null :
            new MediaDesc(infoBackgroundHash.clone(), infoBackgroundMimeType);
        extras.detailBackground = detailBackgroundHash == null ? null :
            new MediaDesc(detailBackgroundHash.clone(), detailBackgroundMimeType);
        extras.peopleBackground = peopleBackgroundHash == null ? null :
            new MediaDesc(peopleBackgroundHash.clone(), peopleBackgroundMimeType);
        extras.charter = charter;
        extras.homepageUrl = homepageUrl;
        return extras;
    }

    /**
     * Checks over the object definitions and will return a map of field, value pairs that contains
     * all of the entries that are not null, and are different from what's in this object
     * currently.  Returns null if the group is not found.
     */
    public Map<String, Object> findUpdates (Group groupDef, GroupExtras extrasDef) 
        throws PersistenceException
    {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        if (groupDef.name != null && !groupDef.name.equals(name)) {
            updates.put(NAME, groupDef.name);
        }
        if (groupDef.blurb != null && !groupDef.blurb.equals(blurb)) {
            updates.put(BLURB, groupDef.blurb);
        }
        if (groupDef.logo != null && (logoMediaHash == null || 
            !groupDef.logo.equals(new MediaDesc(logoMediaHash, logoMimeType, 
            logoMediaConstraint)))) {
            updates.put(LOGO_MEDIA_HASH, groupDef.logo.hash);
            updates.put(LOGO_MIME_TYPE, groupDef.logo.mimeType);
            updates.put(LOGO_MEDIA_CONSTRAINT, groupDef.logo.constraint);
        }
        if (groupDef.policy != policy) {
            updates.put(POLICY, groupDef.policy);
        }
        if (extrasDef.infoBackground != null && (infoBackgroundHash == null ||
            !extrasDef.infoBackground.equals(new MediaDesc(infoBackgroundHash, 
            infoBackgroundMimeType)))) {
            updates.put(INFO_BACKGROUND_HASH, extrasDef.infoBackground.hash);
            updates.put(INFO_BACKGROUND_MIME_TYPE, extrasDef.infoBackground.mimeType);
        }
        if (extrasDef.detailBackground != null && (detailBackgroundHash == null ||
            !extrasDef.detailBackground.equals(new MediaDesc(detailBackgroundHash,
            detailBackgroundMimeType)))) {
            updates.put(DETAIL_BACKGROUND_HASH, extrasDef.detailBackground.hash);
            updates.put(DETAIL_BACKGROUND_MIME_TYPE, extrasDef.detailBackground.mimeType);
        }
        if (extrasDef.peopleBackground != null && (peopleBackgroundHash == null ||
            !extrasDef.peopleBackground.equals(new MediaDesc(peopleBackgroundHash,
            peopleBackgroundMimeType)))) {
            updates.put(PEOPLE_BACKGROUND_HASH, extrasDef.peopleBackground.hash);
            updates.put(PEOPLE_BACKGROUND_MIME_TYPE, extrasDef.peopleBackground.mimeType);
        }
        if (extrasDef.charter != null && !extrasDef.charter.equals(charter)) {
            updates.put(CHARTER, extrasDef.charter);
        }
        if (extrasDef.homepageUrl != null && !extrasDef.homepageUrl.equals(homepageUrl)) {
            updates.put(HOMEPAGE_URL, extrasDef.homepageUrl);
        }
    
        return updates;
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
