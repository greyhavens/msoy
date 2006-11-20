//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Tag records for Pet. */
@Entity
@Table
public class PetTagRecord extends TagRecord<PetRecord>
{
}
