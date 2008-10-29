//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * An experience a member has had in Whirled.  This is used to keep track of the last actions
 * taken by a member.  Only a small number defined by MemberLogic.MAX_EXPERIENCES will be 
 * maintained for each member.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Entity(indices={
    @Index(name="ixMemberId", fields={ MemberExperienceRecord.MEMBER_ID }),
    @Index(name="ixDateOccurred", fields={ MemberExperienceRecord.DATE_OCCURRED })
})
public class MemberExperienceRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #id} field. */
    public static final String ID = "id";

    /** The qualified column identifier for the {@link #id} field. */
    public static final ColumnExp ID_C =
        new ColumnExp(MemberExperienceRecord.class, ID);

    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MemberExperienceRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #dateOccurred} field. */
    public static final String DATE_OCCURRED = "dateOccurred";

    /** The qualified column identifier for the {@link #dateOccurred} field. */
    public static final ColumnExp DATE_OCCURRED_C =
        new ColumnExp(MemberExperienceRecord.class, DATE_OCCURRED);

    /** The column identifier for the {@link #action} field. */
    public static final String ACTION = "action";

    /** The qualified column identifier for the {@link #action} field. */
    public static final ColumnExp ACTION_C =
        new ColumnExp(MemberExperienceRecord.class, ACTION);

    /** The column identifier for the {@link #data} field. */
    public static final String DATA = "data";

    /** The qualified column identifier for the {@link #data} field. */
    public static final ColumnExp DATA_C =
        new ColumnExp(MemberExperienceRecord.class, DATA);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;
    
    /** Unique ID of this experience. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;
    
    /** ID of the member who had the experience. */
    public int memberId;
    
    /** Date/time the experience occurred. */
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
                new String[] { ID },
                new Comparable[] { id });
    }
    // AUTO-GENERATED: METHODS END
}
