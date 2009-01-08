//
// $Id$

package com.threerings.msoy.group.server.persist;

import java.util.Map;

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
@Entity(fullTextIndices={
    @FullTextIndex(name=GroupRecord.FTS_NBC, fields={ "name", "blurb", "charter" })
})
public class GroupRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GroupRecord> _R = GroupRecord.class;
    public static final ColumnExp GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp HOMEPAGE_URL = colexp(_R, "homepageUrl");
    public static final ColumnExp BLURB = colexp(_R, "blurb");
    public static final ColumnExp CHARTER = colexp(_R, "charter");
    public static final ColumnExp LOGO_MEDIA_HASH = colexp(_R, "logoMediaHash");
    public static final ColumnExp LOGO_MIME_TYPE = colexp(_R, "logoMimeType");
    public static final ColumnExp LOGO_MEDIA_CONSTRAINT = colexp(_R, "logoMediaConstraint");
    public static final ColumnExp CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp HOME_SCENE_ID = colexp(_R, "homeSceneId");
    public static final ColumnExp CREATION_DATE = colexp(_R, "creationDate");
    public static final ColumnExp POLICY = colexp(_R, "policy");
    public static final ColumnExp FORUM_PERMS = colexp(_R, "forumPerms");
    public static final ColumnExp PARTY_PERMS = colexp(_R, "partyPerms");
    public static final ColumnExp MEMBER_COUNT = colexp(_R, "memberCount");
    public static final ColumnExp CATALOG_ITEM_TYPE = colexp(_R, "catalogItemType");
    public static final ColumnExp CATALOG_TAG = colexp(_R, "catalogTag");
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp OFFICIAL = colexp(_R, "official");
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
    @Index(name="ixPolicy")
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
    @Index(name="ixOfficial")
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
    public Map<ColumnExp, Object> findUpdates (Group groupDef, GroupExtras extrasDef)
    {
        Map<ColumnExp, Object> updates = Maps.newHashMap();
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
                new ColumnExp[] { GROUP_ID },
                new Comparable[] { groupId });
    }
    // AUTO-GENERATED: METHODS END
}
