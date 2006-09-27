//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

/** Rating records for Furniture. */
@Entity
@Table
public class FurnitureRatingRecord extends RatingRecord<FurnitureRecord>
{
}
