//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Tag History for Furniture. */
@Entity
@Table
public class FurnitureTagHistoryRecord extends TagHistoryRecord<FurnitureRecord>
{
}
