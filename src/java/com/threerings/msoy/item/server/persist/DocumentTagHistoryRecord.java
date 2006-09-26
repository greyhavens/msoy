//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

/** Tag History for Documents. */
@Entity
@Table
public class DocumentTagHistoryRecord extends TagHistoryRecord<DocumentRecord>
{
}
