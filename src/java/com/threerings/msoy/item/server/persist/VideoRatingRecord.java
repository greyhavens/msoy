//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Rating records for Videos. */
@Entity
@Table
public class VideoRatingRecord extends RatingRecord<VideoRecord>
{
}
