//
// $Id$

package com.threerings.msoy.server.persist;

import javax.persistence.*; // for EJB3 annotations

/**
 * Represents a friendship between two members.
 */
@Entity
@Table(uniqueConstraints =
       {@UniqueConstraint(columnNames={"inviterId", "inviteeId"})})
public class FriendRecord
{
    public static final int SCHEMA_VERSION = 1;
    
    public static final String INVITER_ID = "inviterId";
    public static final String INVITEE_ID = "inviteeId";

    /** The member id of the inviter. */
    @Id
    public int inviterId;

    /** The member id of the invitee. */
    @Id
    public int inviteeId;

    /** true if this friendship has been agreed to, false if it pends. */
    @Column(nullable=false)
    public boolean status;
}
