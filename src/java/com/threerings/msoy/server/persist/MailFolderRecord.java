//
// $Id$

package com.threerings.msoy.server.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.samskivert.util.StringUtil;

/**
 * Represents a named folder belonging to a member.
 */
@Entity
@Table(uniqueConstraints={
    @UniqueConstraint(columnNames={MailFolderRecord.MEMBER_ID, MailFolderRecord.NAME })})
public class MailFolderRecord
    implements Cloneable
{
    public static final int SCHEMA_VERSION = 1;

    public static final String FOLDER_ID = "folderId";
    public static final String MEMBER_ID = "ownerId";
    public static final String NAME = "name";
    public static final String NEXT_MESSAGE_ID = "nextMessageId";

    /** The id of this folder, unique relative to this member. */
    @Id
    public int folderId;

    /** The id of the member who owns this folder. */
    @Id
    public int ownerId;
    
    /** The name of this folder. */
    @Column(nullable=false)
    public String name;

    /** The next available message id within this folder. */
    @Column(nullable=false)
    public int nextMessageId;
    
    /**
     * Generates a string representation of this instance.
     */
    @Override
    public String toString ()
    {
        StringBuilder buf = new StringBuilder("[");
        StringUtil.fieldsToString(buf, this);
        return buf.append("]").toString();
    }
}
