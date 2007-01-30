//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

import com.threerings.msoy.server.persist.TagHistoryRecord;

/** Tag History for Documents. */
@Entity
@Table
public class DocumentTagHistoryRecord extends TagHistoryRecord
{
}
