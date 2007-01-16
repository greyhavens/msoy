//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Represents a friendship between two members.
 */
@Entity
@Table(uniqueConstraints =
       {@UniqueConstraint(columnNames={FriendRecord.INVITER_ID, FriendRecord.INVITEE_ID })})
public class FriendRecord extends PersistentRecord
{
    public static final int SCHEMA_VERSION = 1;
    
    public static final String INVITER_ID = "inviterId";
    public static final ColumnExp INVITER_ID_C = new ColumnExp(FriendRecord.class, INVITER_ID);
    public static final String INVITEE_ID = "inviteeId";
    public static final ColumnExp INVITEE_ID_C = new ColumnExp(FriendRecord.class, INVITEE_ID);
    public static final String STATUS = "status";
    public static final ColumnExp STATUS_C = new ColumnExp(FriendRecord.class, STATUS);

    /** The member id of the inviter. */
    @Id
    public int inviterId;

    /** The member id of the invitee. */
    @Id
    public int inviteeId;

    /** true if this friendship has been agreed to, false if it pends. */
    public boolean status;
}
