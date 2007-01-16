//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;


/**
 * A trivial object to count the number of members in a group.
 */
@Computed
@Entity
public class GroupMembershipCount extends PersistentRecord
{
    public static final String COUNT = "count";

    /** The number of members. */
    public int count;
}
