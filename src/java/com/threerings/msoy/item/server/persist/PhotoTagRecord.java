//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Tag records for Photos. */
@Entity
@Table
public class PhotoTagRecord extends TagRecord<PhotoRecord>
{
}
