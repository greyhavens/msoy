//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Tag History for Games. */
@Entity
@Table
public class GameTagHistoryRecord extends TagHistoryRecord<GameRecord>
{
}
