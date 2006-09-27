//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

/** Rating records for Documents. */
@Entity
@Table
public class DocumentRatingRecord extends RatingRecord<DocumentRecord>
{
}
