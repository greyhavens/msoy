//
// $Id$

package com.threerings.msoy.server.persist;

import javax.persistence.Entity;

import com.samskivert.jdbc.depot.Computed;

/**
 * A trivial object to count the number of members in a group.
 */
@Computed
@Entity
public class GroupMembershipCount
    implements Cloneable
{
    public static final String COUNT = "count";

    /** The number of members. */
    public int count;
}
