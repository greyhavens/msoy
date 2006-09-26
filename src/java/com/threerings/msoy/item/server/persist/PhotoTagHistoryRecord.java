//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

/** Tag History for Photos. */
@Entity
@Table
public class PhotoTagHistoryRecord extends TagHistoryRecord<PhotoRecord>
{
}
