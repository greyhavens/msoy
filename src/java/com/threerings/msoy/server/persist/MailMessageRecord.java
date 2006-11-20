//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Table;

import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.util.StringUtil;

/**
 * Represents a message, with some meta-data, in a folder belonging to a member.
 * From this record is generated {@link MailHeaders}, {@link MailMessage} and in
 * some cases, a {@link MailPayloadDisplay}.
 * 
 * TODO: Should we allow multiple recipients? It can be good to know who else received
 *       a certain message, and 'reply all' can be a wonderful feature.
 * TODO: How do we deal with messages that do not have a real sender? System messages of
 *       various kinds, for example.
 */
@Entity
@Table
public class MailMessageRecord
    implements Cloneable
{
    public static final int SCHEMA_VERSION = 3;

    public static final String MESSAGE_ID = "messageId";
    public static final ColumnExp MESSAGE_ID_C =
        new ColumnExp(MailMessageRecord.class, MESSAGE_ID);
    public static final String FOLDER_ID = "folderId";
    public static final ColumnExp FOLDER_ID_C =
        new ColumnExp(MailMessageRecord.class, FOLDER_ID);
    public static final String OWNER_ID = "ownerId";
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(MailMessageRecord.class, OWNER_ID);
    public static final String SENDER_ID = "senderId";
    public static final ColumnExp SENDER_ID_C =
        new ColumnExp(MailMessageRecord.class, "senderId");
    public static final String RECIPIENT_ID = "recipientId";
    public static final ColumnExp RECIPIENT_ID_C =
        new ColumnExp(MailMessageRecord.class, RECIPIENT_ID);
    public static final String SUBJECT = "subject";
    public static final ColumnExp SUBJECT_C =
        new ColumnExp(MailMessageRecord.class, SUBJECT);
    public static final String SENT = "sent";
    public static final ColumnExp SENT_C =
        new ColumnExp(MailMessageRecord.class, SENT);
    public static final String UNREAD = "unread";
    public static final ColumnExp UNREAD_C =
        new ColumnExp(MailMessageRecord.class, UNREAD);
    public static final String BODY_TEXT = "bodyText";
    public static final ColumnExp BODY_TEXT_C =
        new ColumnExp(MailMessageRecord.class, BODY_TEXT);
    public static final String PAYLOAD_TYPE = "payloadType";
    public static final ColumnExp PAYLOAD_TYPE_C =
        new ColumnExp(MailMessageRecord.class, PAYLOAD_TYPE);
    public static final String PAYLOAD_STATE = "payloadState";
    public static final ColumnExp PAYLOAD_STATE_C =
        new ColumnExp(MailMessageRecord.class, PAYLOAD_STATE);
    
    /** The id of this message, unique within its current folder. */
    @Id
    public int messageId;

    /** The id of the folder in which this message is currently filed. */
    @Id
    public int folderId;

    /** The id of the owner of this message. */
    @Id
    public int ownerId;

    /** The id of the sender. */
    public int senderId;

    /** The id of the recipient. */
    public int recipientId;

    /** The subject of this message. */
    public String subject;
    
    /** The time at which the message was delivered. */
    public Timestamp sent;

    /** Whether or not this message is yet to be read. */
    public boolean unread;
    
    /** The text part of the message body, possibly null. */
    @Column(length=32768, nullable=true)
    public String bodyText;

    /** An integer specifying which type of {@link MailPayload} object this message includes. */
    public int payloadType;

    /** The low-level representation of the state of a {@link MailPayload} object. */
    @Column(length=16384, nullable=true)
    public byte[] payloadState;
    
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
