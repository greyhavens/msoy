//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Tag records for Documents. */
@Entity
@Table
public class DocumentTagRecord extends TagRecord<DocumentRecord>
{
}
