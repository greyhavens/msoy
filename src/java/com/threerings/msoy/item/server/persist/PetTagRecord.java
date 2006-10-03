//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

/** Tag records for Pet. */
@Entity
@Table
public class PetTagRecord extends TagRecord<PetRecord>
{
}
