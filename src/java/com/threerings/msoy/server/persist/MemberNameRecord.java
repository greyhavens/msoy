//
// $Id$

package com.threerings.msoy.server.persist;

import java.io.Serializable;

import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * A computed persistent entity that's used to fetch (and cache) member name information only.
 */
@Computed
@Entity
public class MemberNameRecord
    implements Cloneable, Serializable
{
    public static final String MEMBER_ID = "memberId";
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MemberNameRecord.class, MEMBER_ID);
    public static final String NAME = "name";
    public static final ColumnExp NAME_C =
        new ColumnExp(MemberNameRecord.class, NAME);
    
    /** This member's unique id. */
    @Id
    public int memberId;

    /** The name by which this member is known in MetaSOY. */
    public String name;
}