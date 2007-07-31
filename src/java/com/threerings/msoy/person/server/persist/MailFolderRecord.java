//
// $Id$

package com.threerings.msoy.person.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
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
    @UniqueConstraint(fieldNames={MailFolderRecord.OWNER_ID, MailFolderRecord.NAME })})
public class MailFolderRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #folderId} field. */
    public static final String FOLDER_ID = "folderId";

    /** The qualified column identifier for the {@link #folderId} field. */
    public static final ColumnExp FOLDER_ID_C =
        new ColumnExp(MailFolderRecord.class, FOLDER_ID);

    /** The column identifier for the {@link #ownerId} field. */
    public static final String OWNER_ID = "ownerId";

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(MailFolderRecord.class, OWNER_ID);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(MailFolderRecord.class, NAME);

    /** The column identifier for the {@link #nextMessageId} field. */
    public static final String NEXT_MESSAGE_ID = "nextMessageId";

    /** The qualified column identifier for the {@link #nextMessageId} field. */
    public static final ColumnExp NEXT_MESSAGE_ID_C =
        new ColumnExp(MailFolderRecord.class, NEXT_MESSAGE_ID);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

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

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #MailFolderRecord}
     * with the supplied key values.
     */
    public static Key<MailFolderRecord> getKey (int folderId, int ownerId)
    {
        return new Key<MailFolderRecord>(
                MailFolderRecord.class,
                new String[] { FOLDER_ID, OWNER_ID },
                new Comparable[] { folderId, ownerId });
    }
    // AUTO-GENERATED: METHODS END
}
