//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

/** Rating records for Games. */
@Entity
@Table
public class GameRatingRecord extends RatingRecord<GameRecord>
{
}
