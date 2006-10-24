//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

/** Rating records for Audios. */
@Entity
@Table
public class AudioRatingRecord extends RatingRecord<AudioRecord>
{
}
