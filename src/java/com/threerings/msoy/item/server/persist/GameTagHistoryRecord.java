//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

/** Tag History for Games. */
@Entity
@Table
public class GameTagHistoryRecord extends TagHistoryRecord<GameRecord>
{
}
