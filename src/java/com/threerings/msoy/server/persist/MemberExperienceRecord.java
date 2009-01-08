//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

/**
 * An experience a member has had in Whirled.  This is used to keep track of the last actions
 * taken by a member.  Only a small number defined by MemberLogic.MAX_EXPERIENCES will be
 * maintained for each member.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Entity
public class MemberExperienceRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MemberExperienceRecord> _R = MemberExperienceRecord.class;
    public static final ColumnExp ID = colexp(_R, "id");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp DATE_OCCURRED = colexp(_R, "dateOccurred");
    public static final ColumnExp ACTION = colexp(_R, "action");
    public static final ColumnExp DATA = colexp(_R, "data");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** Unique ID of this experience. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    /** ID of the member who had the experience. */
    @Index(name="ixMemberId")
    public int memberId;

    /** Date/time the experience occurred. */
    @Index(name="ixDateOccurred")
    public Timestamp dateOccurred;

    /** Action the member had, as defined by HomePageItem. */
    public byte action;

    /**
     * Data associated with the action, which can be one of the following:
     * <ul>
     *     <li>ACTION_GAME: The ID of the game that was played.</li>
     *     <li>ACTION_ROOM: The ID of the scene that was visited.</li>
     * </ul>
     */
    public String data;

    /** For depot */
    public MemberExperienceRecord () { }

    /**
     * Constructs a new member experience with the given data.
     */
    public MemberExperienceRecord (int memberId, Date dateOccurred, byte action, String data)
    {
        this.memberId = memberId;
        this.dateOccurred = new Timestamp(dateOccurred.getTime());
        this.action = action;
        this.data = data;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MemberExperienceRecord}
     * with the supplied key values.
     */
    public static Key<MemberExperienceRecord> getKey (int id)
    {
        return new Key<MemberExperienceRecord>(
                MemberExperienceRecord.class,
                new ColumnExp[] { ID },
                new Comparable[] { id });
    }
    // AUTO-GENERATED: METHODS END
}
