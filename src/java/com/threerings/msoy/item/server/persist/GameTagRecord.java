//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

/** Tag records for Games. */
@Entity
@Table
public class GameTagRecord extends TagRecord<GameRecord>
{
}
