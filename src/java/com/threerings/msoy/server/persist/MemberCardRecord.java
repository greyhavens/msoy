//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
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
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MemberCardRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(MemberCardRecord.class, NAME);

    /** The column identifier for the {@link #lastSession} field. */
    public static final String LAST_SESSION = "lastSession";

    /** The qualified column identifier for the {@link #lastSession} field. */
    public static final ColumnExp LAST_SESSION_C =
        new ColumnExp(MemberCardRecord.class, LAST_SESSION);

    /** The column identifier for the {@link #photoHash} field. */
    public static final String PHOTO_HASH = "photoHash";

    /** The qualified column identifier for the {@link #photoHash} field. */
    public static final ColumnExp PHOTO_HASH_C =
        new ColumnExp(MemberCardRecord.class, PHOTO_HASH);

    /** The column identifier for the {@link #photoMimeType} field. */
    public static final String PHOTO_MIME_TYPE = "photoMimeType";

    /** The qualified column identifier for the {@link #photoMimeType} field. */
    public static final ColumnExp PHOTO_MIME_TYPE_C =
        new ColumnExp(MemberCardRecord.class, PHOTO_MIME_TYPE);

    /** The column identifier for the {@link #photoConstraint} field. */
    public static final String PHOTO_CONSTRAINT = "photoConstraint";

    /** The qualified column identifier for the {@link #photoConstraint} field. */
    public static final ColumnExp PHOTO_CONSTRAINT_C =
        new ColumnExp(MemberCardRecord.class, PHOTO_CONSTRAINT);

    /** The column identifier for the {@link #headline} field. */
    public static final String HEADLINE = "headline";

    /** The qualified column identifier for the {@link #headline} field. */
    public static final ColumnExp HEADLINE_C =
        new ColumnExp(MemberCardRecord.class, HEADLINE);

    /** The column identifier for the {@link #level} field. */
    public static final String LEVEL = "level";

    /** The qualified column identifier for the {@link #level} field. */
    public static final ColumnExp LEVEL_C =
        new ColumnExp(MemberCardRecord.class, LEVEL);
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
        if (photoHash != null) {
            card.photo = new MediaDesc(photoHash, photoMimeType, photoConstraint);
        }
        card.headline = headline;
        MemberCard.NotOnline status = new MemberCard.NotOnline();
        status.lastLogon = (lastSession == null) ? 0L : lastSession.getTime();
        card.status = status;
        card.level = level;
        return card;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MemberCardRecord}
     * with the supplied key values.
     */
    public static Key<MemberCardRecord> getKey (int memberId)
    {
        return new Key<MemberCardRecord>(
                MemberCardRecord.class,
                new String[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
