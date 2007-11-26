//
// $Id$

package com.threerings.msoy.group.server.persist;

import java.util.Map;
import java.util.HashMap;

import java.sql.Date;

import com.google.common.collect.Maps;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.FullTextIndex;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.io.PersistenceException;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.group.data.GroupExtras;

/**
 * Contains the details of a group.
 */
@Entity(indices={
    @Index(name="ixPolicy", fields={ GroupRecord.POLICY })
})
@Table(fullTextIndexes={
    @FullTextIndex(name=GroupRecord.FTS_NBC, fieldNames={
        GroupRecord.NAME, GroupRecord.BLURB, GroupRecord.CHARTER })
})
public class GroupRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #groupId} field. */
    public static final String GROUP_ID = "groupId";

    /** The qualified column identifier for the {@link #groupId} field. */
    public static final ColumnExp GROUP_ID_C =
        new ColumnExp(GroupRecord.class, GROUP_ID);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(GroupRecord.class, NAME);

    /** The column identifier for the {@link #homepageUrl} field. */
    public static final String HOMEPAGE_URL = "homepageUrl";

    /** The qualified column identifier for the {@link #homepageUrl} field. */
    public static final ColumnExp HOMEPAGE_URL_C =
        new ColumnExp(GroupRecord.class, HOMEPAGE_URL);

    /** The column identifier for the {@link #blurb} field. */
    public static final String BLURB = "blurb";

    /** The qualified column identifier for the {@link #blurb} field. */
    public static final ColumnExp BLURB_C =
        new ColumnExp(GroupRecord.class, BLURB);

    /** The column identifier for the {@link #charter} field. */
    public static final String CHARTER = "charter";

    /** The qualified column identifier for the {@link #charter} field. */
    public static final ColumnExp CHARTER_C =
        new ColumnExp(GroupRecord.class, CHARTER);

    /** The column identifier for the {@link #logoMediaHash} field. */
    public static final String LOGO_MEDIA_HASH = "logoMediaHash";

    /** The qualified column identifier for the {@link #logoMediaHash} field. */
    public static final ColumnExp LOGO_MEDIA_HASH_C =
        new ColumnExp(GroupRecord.class, LOGO_MEDIA_HASH);

    /** The column identifier for the {@link #logoMimeType} field. */
    public static final String LOGO_MIME_TYPE = "logoMimeType";

    /** The qualified column identifier for the {@link #logoMimeType} field. */
    public static final ColumnExp LOGO_MIME_TYPE_C =
        new ColumnExp(GroupRecord.class, LOGO_MIME_TYPE);

    /** The column identifier for the {@link #logoMediaConstraint} field. */
    public static final String LOGO_MEDIA_CONSTRAINT = "logoMediaConstraint";

    /** The qualified column identifier for the {@link #logoMediaConstraint} field. */
    public static final ColumnExp LOGO_MEDIA_CONSTRAINT_C =
        new ColumnExp(GroupRecord.class, LOGO_MEDIA_CONSTRAINT);

    /** The column identifier for the {@link #backgroundControl} field. */
    public static final String BACKGROUND_CONTROL = "backgroundControl";

    /** The qualified column identifier for the {@link #backgroundControl} field. */
    public static final ColumnExp BACKGROUND_CONTROL_C =
        new ColumnExp(GroupRecord.class, BACKGROUND_CONTROL);

    /** The column identifier for the {@link #backgroundMimeType} field. */
    public static final String BACKGROUND_MIME_TYPE = "backgroundMimeType";

    /** The qualified column identifier for the {@link #backgroundMimeType} field. */
    public static final ColumnExp BACKGROUND_MIME_TYPE_C =
        new ColumnExp(GroupRecord.class, BACKGROUND_MIME_TYPE);

    /** The column identifier for the {@link #backgroundHash} field. */
    public static final String BACKGROUND_HASH = "backgroundHash";

    /** The qualified column identifier for the {@link #backgroundHash} field. */
    public static final ColumnExp BACKGROUND_HASH_C =
        new ColumnExp(GroupRecord.class, BACKGROUND_HASH);

    /** The column identifier for the {@link #backgroundThumbConstraint} field. */
    public static final String BACKGROUND_THUMB_CONSTRAINT = "backgroundThumbConstraint";

    /** The qualified column identifier for the {@link #backgroundThumbConstraint} field. */
    public static final ColumnExp BACKGROUND_THUMB_CONSTRAINT_C =
        new ColumnExp(GroupRecord.class, BACKGROUND_THUMB_CONSTRAINT);

    /** The column identifier for the {@link #creatorId} field. */
    public static final String CREATOR_ID = "creatorId";

    /** The qualified column identifier for the {@link #creatorId} field. */
    public static final ColumnExp CREATOR_ID_C =
        new ColumnExp(GroupRecord.class, CREATOR_ID);

    /** The column identifier for the {@link #homeSceneId} field. */
    public static final String HOME_SCENE_ID = "homeSceneId";

    /** The qualified column identifier for the {@link #homeSceneId} field. */
    public static final ColumnExp HOME_SCENE_ID_C =
        new ColumnExp(GroupRecord.class, HOME_SCENE_ID);

    /** The column identifier for the {@link #creationDate} field. */
    public static final String CREATION_DATE = "creationDate";

    /** The qualified column identifier for the {@link #creationDate} field. */
    public static final ColumnExp CREATION_DATE_C =
        new ColumnExp(GroupRecord.class, CREATION_DATE);

    /** The column identifier for the {@link #policy} field. */
    public static final String POLICY = "policy";

    /** The qualified column identifier for the {@link #policy} field. */
    public static final ColumnExp POLICY_C =
        new ColumnExp(GroupRecord.class, POLICY);

    /** The column identifier for the {@link #memberCount} field. */
    public static final String MEMBER_COUNT = "memberCount";

    /** The qualified column identifier for the {@link #memberCount} field. */
    public static final ColumnExp MEMBER_COUNT_C =
        new ColumnExp(GroupRecord.class, MEMBER_COUNT);
    // AUTO-GENERATED: FIELDS END

    /** The identifier for the full text search index on Name, Blurb, Charter */
    public static final String FTS_NBC = "NBC";

    public static final int SCHEMA_VERSION = 16;

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
    @Column(length=Group.MAX_BLURB_LENGTH, nullable=true)
    public String blurb;

    /** The group's charter, or null if one has yet to be set. */
    @Column(length=Group.MAX_CHARTER_LENGTH, nullable=true)
    public String charter;

    /** A hash code identifying the media for this group's logo. */
    @Column(nullable=true)
    public byte[] logoMediaHash;

    /** The MIME type of this group's logo. */
    public byte logoMimeType;

    /** The constraint for the logo image. */
    public byte logoMediaConstraint;

    /** Flag to indicate page flow control */
    public int backgroundControl;

    /** The MIME type for the background of the info area. */
    public byte backgroundMimeType;

    /** A hash code identifying the media for the background of the info area. */
    @Column(nullable=true)
    public byte[] backgroundHash;

    /** The constraint for the thumbnail of this image. */
    public byte backgroundThumbConstraint;

    /** The member id of the person who created the group. */
    public int creatorId;

    /** The home scene of this group. */
    public int homeSceneId;

    /** The date and time this group was created. */
    public Date creationDate;

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
        if (logoMediaHash != null) {
            group.logo = new MediaDesc(logoMediaHash, logoMimeType, logoMediaConstraint);
        }
        group.creatorId = creatorId;
        group.creationDate = new java.util.Date(creationDate.getTime());
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
        extras.charter = charter;
        extras.homepageUrl = homepageUrl;
        extras.backgroundControl = backgroundControl;
        if (backgroundHash != null) {
            extras.background = new MediaDesc(
                backgroundHash, backgroundMimeType, backgroundThumbConstraint);
        }
        return extras;
    }

    /**
     * Creates a web-safe version of the group name.
     */
    public GroupName toGroupName ()
    {
        return new GroupName(name, groupId);
    }

    /**
     * Checks over the object definitions and returns a map of field/value pairs that contains all
     * of the entries that are non-null and different from what's in the object currently. Returns
     * null if the group is not found.
     */
    public Map<String, Object> findUpdates (Group groupDef, GroupExtras extrasDef)
        throws PersistenceException
    {
        HashMap<String, Object> updates = Maps.newHashMap();
        if (groupDef.name != null && !groupDef.name.equals(name)) {
            updates.put(NAME, groupDef.name);
        }
        if (groupDef.blurb != null && !groupDef.blurb.equals(blurb)) {
            updates.put(BLURB, groupDef.blurb);
        }
        if (groupDef.logo != null && (logoMediaHash == null || !groupDef.logo.equals(
                new MediaDesc(logoMediaHash, logoMimeType, logoMediaConstraint)))) {
            updates.put(LOGO_MEDIA_HASH, groupDef.logo.hash);
            updates.put(LOGO_MIME_TYPE, groupDef.logo.mimeType);
            updates.put(LOGO_MEDIA_CONSTRAINT, groupDef.logo.constraint);
        }
        if (groupDef.policy != policy) {
            updates.put(POLICY, groupDef.policy);
        }
        if (extrasDef.charter != null && !extrasDef.charter.equals(charter)) {
            updates.put(CHARTER, extrasDef.charter);
        }
        if (extrasDef.homepageUrl != null && !extrasDef.homepageUrl.equals(homepageUrl)) {
            updates.put(HOMEPAGE_URL, extrasDef.homepageUrl);
        }
        if (extrasDef.backgroundControl != backgroundControl) {
            updates.put(BACKGROUND_CONTROL, extrasDef.backgroundControl);
        }
        if (extrasDef.background != null && (backgroundHash == null || !extrasDef.background.equals(
                new MediaDesc(backgroundHash, backgroundMimeType, backgroundThumbConstraint)))) {
            updates.put(BACKGROUND_HASH, extrasDef.background.hash);
            updates.put(BACKGROUND_MIME_TYPE, extrasDef.background.mimeType);
            // the thumbnail instead of photo constraint is stored here - see GroupEdit
            updates.put(BACKGROUND_THUMB_CONSTRAINT, extrasDef.background.constraint);
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

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #GroupRecord}
     * with the supplied key values.
     */
    public static Key<GroupRecord> getKey (int groupId)
    {
        return new Key<GroupRecord>(
                GroupRecord.class,
                new String[] { GROUP_ID },
                new Comparable[] { groupId });
    }
    // AUTO-GENERATED: METHODS END
}
