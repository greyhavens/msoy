//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Rating records for Games. */
@Entity
@Table
public class GameRatingRecord extends RatingRecord<GameRecord>
{
}
