//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

/** Rating records for Photos. */
@Entity
@Table
public class PhotoRatingRecord extends RatingRecord<PhotoRecord>
{
}
