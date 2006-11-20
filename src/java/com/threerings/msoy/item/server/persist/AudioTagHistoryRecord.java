//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Tag History for Audios. */
@Entity
@Table
public class AudioTagHistoryRecord extends TagHistoryRecord<AudioRecord>
{
}
