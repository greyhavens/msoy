//
// $Id$

package com.threerings.msoy.person.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations

/**
 * Used to load profile photo information from the {@link ProfileRecord} table.
 */
@Computed(shadowOf=ProfileRecord.class)
@Entity
public class ProfilePhotoRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(ProfilePhotoRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #photoHash} field. */
    public static final String PHOTO_HASH = "photoHash";

    /** The qualified column identifier for the {@link #photoHash} field. */
    public static final ColumnExp PHOTO_HASH_C =
        new ColumnExp(ProfilePhotoRecord.class, PHOTO_HASH);

    /** The column identifier for the {@link #photoMimeType} field. */
    public static final String PHOTO_MIME_TYPE = "photoMimeType";

    /** The qualified column identifier for the {@link #photoMimeType} field. */
    public static final ColumnExp PHOTO_MIME_TYPE_C =
        new ColumnExp(ProfilePhotoRecord.class, PHOTO_MIME_TYPE);

    /** The column identifier for the {@link #photoConstraint} field. */
    public static final String PHOTO_CONSTRAINT = "photoConstraint";

    /** The qualified column identifier for the {@link #photoConstraint} field. */
    public static final ColumnExp PHOTO_CONSTRAINT_C =
        new ColumnExp(ProfilePhotoRecord.class, PHOTO_CONSTRAINT);
    // AUTO-GENERATED: FIELDS END

    /** the member's unique id. */
    @Id public int memberId;

    /** The hash code of the user's profile photo. */
    @Column(nullable=true)
    public byte[] photoHash;

    /** The MIME type of photo image. */
    public byte photoMimeType;

    /** The constraint for the photo image. */
    public byte photoConstraint;
}
