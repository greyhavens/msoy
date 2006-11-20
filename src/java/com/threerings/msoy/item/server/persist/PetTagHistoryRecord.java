//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Tag History for Pet. */
@Entity
@Table
public class PetTagHistoryRecord extends TagHistoryRecord<PetRecord>
{
}
