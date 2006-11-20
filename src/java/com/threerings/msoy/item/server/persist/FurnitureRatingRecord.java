//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Rating records for Furniture. */
@Entity
@Table
public class FurnitureRatingRecord extends RatingRecord<FurnitureRecord>
{
}
