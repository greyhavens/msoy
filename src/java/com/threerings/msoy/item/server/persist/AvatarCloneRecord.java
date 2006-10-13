//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/** Clone records for Avatars. */
@Entity
@Table
@TableGenerator(name="cloneId", allocationSize=-1,
                initialValue=-1, pkColumnValue="AVATAR_CLONE")
public class AvatarCloneRecord extends CloneRecord<AvatarRecord>
{
}
