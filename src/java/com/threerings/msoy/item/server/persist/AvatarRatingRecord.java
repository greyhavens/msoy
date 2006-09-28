//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

/** Rating records for Avatars. */
@Entity
@Table
public class AvatarRatingRecord extends RatingRecord<AvatarRecord>
{
}
