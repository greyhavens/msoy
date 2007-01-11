//
// $Id$

package com.threerings.msoy.server.persist;

import java.io.Serializable;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.UniqueConstraint;

import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.util.StringUtil;

/**
 * Represents a named folder belonging to a member.
 */
@Entity
@Table(uniqueConstraints={
    @UniqueConstraint(columnNames={MailFolderRecord.OWNER_ID, MailFolderRecord.NAME })})
public class MailFolderRecord
    implements Cloneable, Serializable
{
    public static final int SCHEMA_VERSION = 1;

    public static final String FOLDER_ID = "folderId";
    public static final ColumnExp FOLDER_ID_C =
        new ColumnExp(MailFolderRecord.class, FOLDER_ID);
    public static final String OWNER_ID = "ownerId";
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(MailFolderRecord.class, OWNER_ID);
    public static final String NAME = "name";
    public static final ColumnExp NAME_C =
        new ColumnExp(MailFolderRecord.class, NAME);
    public static final String NEXT_MESSAGE_ID = "nextMessageId";
    public static final ColumnExp NEXT_MESSAGE_ID_C =
        new ColumnExp(MailFolderRecord.class, NEXT_MESSAGE_ID);

    /** The id of this folder, unique relative to this member. */
    @Id
    public int folderId;

    /** The id of the member who owns this folder. */
    @Id
    public int ownerId;
    
    /** The name of this folder. */
    public String name;

    /** The next available message id within this folder. */
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
