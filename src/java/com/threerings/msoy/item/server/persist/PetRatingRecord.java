//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

/** Rating records for Pet. */
@Entity
@Table
public class PetRatingRecord extends RatingRecord<PetRecord>
{
}
