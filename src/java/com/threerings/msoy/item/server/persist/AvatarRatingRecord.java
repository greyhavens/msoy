//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Rating records for Avatars. */
@Entity
@Table
public class AvatarRatingRecord extends RatingRecord<AvatarRecord>
{
}
