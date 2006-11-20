//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Rating records for Pet. */
@Entity
@Table
public class PetRatingRecord extends RatingRecord<PetRecord>
{
}
