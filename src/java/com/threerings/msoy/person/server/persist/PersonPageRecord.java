//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.util.List;

import javax.persistence.*; // for EJB3 annotations

/**
 * Contains the configuration of a particular member's person page.
 */
@Entity public class PersonPageRecord
{
    /** The id of the member whose person page this describes. */
    @Id public int memberId;

    /** Defines the order of the blurbs on the page. */
    public int[] blurbOrder;

    /** Metadata for all of the blurbs on this player's page. */
    @OneToMany
    @JoinColumn(name="memberId")
    public List<BlurbRecord> blurbs;
}
