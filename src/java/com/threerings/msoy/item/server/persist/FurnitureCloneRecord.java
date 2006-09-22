//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/** Clone records for Furniture. */
@Entity
@Table
@TableGenerator(
    name="itemId",
    allocationSize=1,
    pkColumnValue="FURNITURE")
public class FurnitureCloneRecord extends CloneRecord<FurnitureRecord>
{
}
