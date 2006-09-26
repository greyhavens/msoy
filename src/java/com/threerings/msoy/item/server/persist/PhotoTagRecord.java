//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

/** Tag records for Photos. */
@Entity
@Table
public class PhotoTagRecord extends TagRecord<PhotoRecord>
{
}
