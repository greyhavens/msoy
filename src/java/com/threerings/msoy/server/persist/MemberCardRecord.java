//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.web.data.MemberCard;

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
    // AUTO-GENERATED: FIELDS END

    /** This member's unique id. */
    @Id @Computed(shadowOf=MemberRecord.class)
    public int memberId;

    /** The name by which this member is known in MetaSOY. */
    @Computed(shadowOf=MemberRecord.class)
    public String name;

    /** The hash code of the user's profile photo. */
    @Computed(shadowOf=ProfileRecord.class)
    public byte[] photoHash;

    /** The MIME type of photo image. */
    @Computed(shadowOf=ProfileRecord.class)
    public byte photoMimeType;

    /** The constraint for the photo image. */
    @Computed(shadowOf=ProfileRecord.class)
    public byte photoConstraint;

    /**
     * Creates a runtime record from this persistent record.
     */
    public MemberCard toMemberCard ()
    {
        MemberCard card = new MemberCard();
        card.name = new MemberName(name, memberId);
        card.photo = (photoHash != null) ?
            new MediaDesc(photoHash, photoMimeType, photoConstraint) : Profile.DEFAULT_PHOTO;
        return card;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #MemberCardRecord}
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
