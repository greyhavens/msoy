//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.web.gwt.MemberCard;

/**
 * A computed persistent entity that's used to fetch (and cache) member name and photo info.
 */
@Computed
@Entity
public class MemberCardRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MemberCardRecord> _R = MemberCardRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp LAST_SESSION = colexp(_R, "lastSession");
    public static final ColumnExp FLAGS = colexp(_R, "flags");
    public static final ColumnExp ACCOUNT_NAME = colexp(_R, "accountName");
    public static final ColumnExp PHOTO_HASH = colexp(_R, "photoHash");
    public static final ColumnExp PHOTO_MIME_TYPE = colexp(_R, "photoMimeType");
    public static final ColumnExp PHOTO_CONSTRAINT = colexp(_R, "photoConstraint");
    public static final ColumnExp HEADLINE = colexp(_R, "headline");
    public static final ColumnExp LEVEL = colexp(_R, "level");
    // AUTO-GENERATED: FIELDS END

    /** This member's unique id. */
    @Id @Computed(shadowOf=MemberRecord.class)
    public int memberId;

    /** The name by which this member is known in MetaSOY. */
    @Computed(shadowOf=MemberRecord.class)
    public String name;

    /** The time at which the player ended their last session. */
    @Computed(shadowOf=MemberRecord.class)
    public Timestamp lastSession;

    /** The flags for this player. */
    @Computed(shadowOf=MemberRecord.class)
    public int flags;

    /** The account name of this player. */
    @Computed(shadowOf=MemberRecord.class)
    public String accountName;

    /** The hash code of the user's profile photo. */
    @Computed(shadowOf=ProfileRecord.class)
    public byte[] photoHash;

    /** The MIME type of photo image. */
    @Computed(shadowOf=ProfileRecord.class)
    public byte photoMimeType;

    /** The constraint for the photo image. */
    @Computed(shadowOf=ProfileRecord.class)
    public byte photoConstraint;

    /** A short bit of text provided by the member. */
    @Computed(shadowOf=ProfileRecord.class)
    public String headline;

    /** The currently reported level of this user. */
    @Computed(shadowOf=MemberRecord.class)
    public int level;

    /**
     * Converts a sequence of records into their runtime form.
     */
    public static List<MemberCard> toMemberCards (Iterable<MemberCardRecord> records)
    {
        return Lists.newArrayList(
            Iterables.transform(records, new Function<MemberCardRecord, MemberCard>() {
            public MemberCard apply (MemberCardRecord record) {
                return record.toMemberCard();
            }
        }));
    }

    /**
     * Converts a sequence of records into a mapping from member id to MemberCard.
     */
    public static Map<Integer, MemberCard> toMap (Iterable<MemberCardRecord> records)
    {
        Map<Integer, MemberCard> cards = Maps.newHashMap();
        for (MemberCardRecord record : records) {
            cards.put(record.memberId, record.toMemberCard());
        }
        return cards;
    }

    /**
     * Creates a runtime record from this persistent record.
     */
    public MemberCard toMemberCard ()
    {
        return toMemberCard(new MemberCard());
    }

    /**
     * Fills in the supplied runtime record with information from this persistent record.
     */
    public <T extends MemberCard> T toMemberCard (T card)
    {
        card.name = new MemberName(name, memberId);
        card.photo = toPhoto();
        card.headline = headline;
        MemberCard.NotOnline status = new MemberCard.NotOnline();
        status.lastLogon = (lastSession == null) ? 0L : lastSession.getTime();
        card.status = status;
        card.level = level;
        card.role = MemberRecord.toRole(memberId, flags, accountName);
        return card;
    }

    /**
     * Convert this MemberCardRecord into a VizMemberName
     */
    public VizMemberName toVizMemberName ()
    {
        return new VizMemberName(name, memberId, toPhoto());
    }

    /**
     * Convert this MemberCardRecord into just the photo for the player.
     */
    public MediaDesc toPhoto ()
    {
        return (photoHash == null)
            ? MemberCard.DEFAULT_PHOTO
            : new MediaDesc(photoHash, photoMimeType, photoConstraint);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MemberCardRecord}
     * with the supplied key values.
     */
    public static Key<MemberCardRecord> getKey (int memberId)
    {
        return newKey(_R, memberId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MEMBER_ID); }
    // AUTO-GENERATED: METHODS END
}
