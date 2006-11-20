//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Tag History for Photos. */
@Entity
@Table
public class PhotoTagHistoryRecord extends TagHistoryRecord<PhotoRecord>
{
}
