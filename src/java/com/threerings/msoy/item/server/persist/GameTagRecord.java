//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Tag records for Games. */
@Entity
@Table
public class GameTagRecord extends TagRecord<GameRecord>
{
}
