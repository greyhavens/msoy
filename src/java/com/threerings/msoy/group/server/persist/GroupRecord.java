//
// $Id$

package com.threerings.msoy.group.server.persist;

import java.util.Map;
import java.util.HashMap;

import java.sql.Date;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.FullTextIndex;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.gwt.GroupExtras;

/**
 * Contains the details of a group.
 */
@Entity(indices={
    @Index(name="ixPolicy", fields={ GroupRecord.POLICY }),
    @Index(name="ixOfficial", fields={ GroupRecord.OFFICIAL })
}, fullTextIndices={
    @FullTextIndex(name=GroupRecord.FTS_NBC, fields={
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

    /** The column identifier for the {@link #forumPerms} field. */
    public static final String FORUM_PERMS = "forumPerms";

    /** The qualified column identifier for the {@link #forumPerms} field. */
    public static final ColumnExp FORUM_PERMS_C =
        new ColumnExp(GroupRecord.class, FORUM_PERMS);

    /** The column identifier for the {@link #partyPerms} field. */
    public static final String PARTY_PERMS = "partyPerms";

    /** The qualified column identifier for the {@link #partyPerms} field. */
    public static final ColumnExp PARTY_PERMS_C =
        new ColumnExp(GroupRecord.class, PARTY_PERMS);

    /** The column identifier for the {@link #memberCount} field. */
    public static final String MEMBER_COUNT = "memberCount";

    /** The qualified column identifier for the {@link #memberCount} field. */
    public static final ColumnExp MEMBER_COUNT_C =
        new ColumnExp(GroupRecord.class, MEMBER_COUNT);

    /** The column identifier for the {@link #catalogItemType} field. */
    public static final String CATALOG_ITEM_TYPE = "catalogItemType";

    /** The qualified column identifier for the {@link #catalogItemType} field. */
    public static final ColumnExp CATALOG_ITEM_TYPE_C =
        new ColumnExp(GroupRecord.class, CATALOG_ITEM_TYPE);

    /** The column identifier for the {@link #catalogTag} field. */
    public static final String CATALOG_TAG = "catalogTag";

    /** The qualified column identifier for the {@link #catalogTag} field. */
    public static final ColumnExp CATALOG_TAG_C =
        new ColumnExp(GroupRecord.class, CATALOG_TAG);

    /** The column identifier for the {@link #gameId} field. */
    public static final String GAME_ID = "gameId";

    /** The qualified column identifier for the {@link #gameId} field. */
    public static final ColumnExp GAME_ID_C =
        new ColumnExp(GroupRecord.class, GAME_ID);

    /** The column identifier for the {@link #official} field. */
    public static final String OFFICIAL = "official";

    /** The qualified column identifier for the {@link #official} field. */
    public static final ColumnExp OFFICIAL_C =
        new ColumnExp(GroupRecord.class, OFFICIAL);
    // AUTO-GENERATED: FIELDS END

    /** The identifier for the full text search index on Name, Blurb, Charter */
    public static final String FTS_NBC = "NBC";

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 22;

    /** Converts a persistent record into a {@link GroupCard}. */
    public static final Function<GroupRecord, GroupCard> TO_CARD =
        new Function<GroupRecord, GroupCard>() {
        public GroupCard apply (GroupRecord record) {
            return record.toGroupCard();
        }
    };

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

    /** The member id of the person who created the group. */
    public int creatorId;

    /** The home scene of this group. */
    public int homeSceneId;

    /** The date and time this group was created. */
    public Date creationDate;

    /** The group may be public, invite-only or exclusive as per {@link Group}. */
    public byte policy;

    /** This group's forum permissions, see {@link Group#forumPerms}. */
    public byte forumPerms;

    /** This group's party permissions. */
    @Column(defaultValue="2") // 2 == PERM_MEMBER
    public byte partyPerms;

    /** The number of people that are currently members of this group. */
    public int memberCount;

    /** The item type of the catalog page to land on with the browse by tag function. */
    public byte catalogItemType;

    /** The catalog tag to use with the browse by tag function. */
    @Column(nullable=true)
    public String catalogTag;

    /** The id of the game associated with this whirled, or 0 if there is none */
    public int gameId;

    /** If the group is an official whirled group. */
    public boolean official;

    /**
     * Creates a web-safe version of this group.
     */
    public Group toGroupObject ()
    {
        Group group = new Group();
        group.groupId = groupId;
        group.name = name;
        group.blurb = blurb;
        group.logo = toLogo();
        group.homeSceneId = homeSceneId;
        group.creatorId = creatorId;
        group.creationDate = new java.util.Date(creationDate.getTime());
        group.policy = policy;
        group.forumPerms = forumPerms;
        group.partyPerms = partyPerms;
        group.memberCount = memberCount;
        group.gameId = gameId;
        group.official = official;
        return group;
    }

    /**
     * Creates a MediaDesc of the group logo, or returns null if there is none.
     */
    public MediaDesc toLogo ()
    {
        if (logoMediaHash == null) {
            return null;
        }
        return new MediaDesc(logoMediaHash, logoMimeType, logoMediaConstraint);
    }

    /**
     * Creates a web-safe version of the extras in this group.
     */
    public GroupExtras toExtrasObject ()
    {
        GroupExtras extras = new GroupExtras();
        extras.charter = charter;
        extras.homepageUrl = homepageUrl;
        extras.catalogItemType = catalogItemType;
        extras.catalogTag = catalogTag;
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
     * Creates a card record for this group. Note: the canonical snapshot image will not be filled
     * in. That must be filled in by the caller with a call to resolveSnapshots().
     */
    public GroupCard toGroupCard ()
    {
        GroupCard card = new GroupCard();
        card.name = toGroupName();
        card.logo = toLogo();
        card.blurb = blurb;
        card.homeSceneId = homeSceneId;
        card.memberCount = memberCount;
        card.official = official;
        return card;
    }

    /**
     * Checks over the object definitions and returns a map of field/value pairs that contains all
     * of the entries that are non-null and different from what's in the object currently. Returns
     * null if the group is not found.
     */
    public Map<String, Object> findUpdates (Group groupDef, GroupExtras extrasDef)
    {
        HashMap<String, Object> updates = Maps.newHashMap();
        if (groupDef.name != null && !groupDef.name.equals(name)) {
            updates.put(NAME, groupDef.name);
        }
        if (groupDef.blurb != null && !groupDef.blurb.equals(blurb)) {
            updates.put(BLURB, groupDef.blurb);
        }
        if (groupDef.logo != null && !groupDef.logo.equals(toLogo())) {
            updates.put(LOGO_MEDIA_HASH, groupDef.logo.hash);
            updates.put(LOGO_MIME_TYPE, groupDef.logo.mimeType);
            updates.put(LOGO_MEDIA_CONSTRAINT, groupDef.logo.constraint);
        }
        if (groupDef.policy != policy) {
            updates.put(POLICY, groupDef.policy);
        }
        if (groupDef.forumPerms != forumPerms) {
            updates.put(FORUM_PERMS, groupDef.forumPerms);
        }
        if (groupDef.partyPerms != partyPerms) {
            updates.put(PARTY_PERMS, groupDef.partyPerms);
        }
        if (groupDef.official != official) {
            updates.put(OFFICIAL, groupDef.official);
        }
        if (extrasDef.charter != null && !extrasDef.charter.equals(charter)) {
            updates.put(CHARTER, extrasDef.charter);
        }
        if (extrasDef.homepageUrl != null && !extrasDef.homepageUrl.equals(homepageUrl)) {
            updates.put(HOMEPAGE_URL, extrasDef.homepageUrl);
        }
        if (extrasDef.catalogItemType != catalogItemType) {
            updates.put(CATALOG_ITEM_TYPE, extrasDef.catalogItemType);
        }
        if (extrasDef.catalogTag != null && !extrasDef.catalogTag.equals(catalogTag)) {
            updates.put(CATALOG_TAG, extrasDef.catalogTag);
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
     * Create and return a primary {@link Key} to identify a {@link GroupRecord}
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
