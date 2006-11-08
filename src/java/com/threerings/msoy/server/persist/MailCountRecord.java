//
// $Id$

package com.threerings.msoy.server.persist;

import javax.persistence.Entity;

import com.samskivert.jdbc.depot.Computed;

/**
 * A computed entity that receives the count(*) results for read and unread messages in a folder.
 */
@Computed
@Entity
public class MailCountRecord
    implements Cloneable
{
    public static final String UNREAD = "unread";
    public static final String COUNT = "count";

    /** Whether this count is an unread count or a read count. */
    public boolean unread;
    
    /** The number of messages that are unread (or read). */
    public int count;
}
