//
// $Id$

package com.threerings.msoy.person.server.persist;

import com.samskivert.depot.annotation.*; // for Depot annotations

/**
 * Contains metadata for a particular blurb contained on a player's person
 * page.
 */
@Entity
public class BlurbRecord
{
    /** A unique identifier assigned to all blurbs. */
    @Id
    public int blurbId;

    /** The member with which this blurb is associated. */
    @Column(nullable=false)
    public int memberId;

    /** The type of blurb in question. */
    @Column(nullable=false)
    public int type;

    /** Arbitrary layout information interpreted by the layout code. */
    @Column(nullable=false)
    public String layoutData;
}
