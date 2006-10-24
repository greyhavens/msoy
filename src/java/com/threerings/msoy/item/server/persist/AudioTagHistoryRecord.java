//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

/** Tag History for Audios. */
@Entity
@Table
public class AudioTagHistoryRecord extends TagHistoryRecord<AudioRecord>
{
}
