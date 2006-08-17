//
// $Id$

package com.threerings.msoy.person.server.persist;

import javax.persistence.*; // for EJB3 annotations

/**
 * Contains metadata for a particular blurb contained on a player's person
 * page.
 */
@Entity public class BlurbRecord
{
    /** A unique identifier assigned to all blurbs. */
    @Id public int blurbId;

    /** The member with which this blurb is associated. */
    // @NotNull
    public int memberId;

    /** The type of blurb in question. */
    // @NotNull
    public int type;

    /** Arbitrary layout information interpreted by the layout code. */
    // @NotNull
    public String layoutData;
}
