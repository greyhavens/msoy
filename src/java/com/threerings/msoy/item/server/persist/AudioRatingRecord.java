//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Rating records for Audios. */
@Entity
@Table
public class AudioRatingRecord extends RatingRecord<AudioRecord>
{
}
