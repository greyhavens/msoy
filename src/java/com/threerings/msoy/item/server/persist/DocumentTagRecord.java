//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

/** Tag records for Documents. */
@Entity
@Table
public class DocumentTagRecord extends TagRecord<DocumentRecord>
{
}
