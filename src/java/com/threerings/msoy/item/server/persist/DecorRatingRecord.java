//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Rating records for Decor. */
@Entity
@Table
public class DecorRatingRecord extends RatingRecord<DecorRecord>
{
}
