//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.samskivert.util.StringUtil;

/**
 * Represents a message, with some meta-data, in a folder belonging to a member.
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
    public static final int SCHEMA_VERSION = 1;

    public static final String MESSAGE_ID = "messageId";
    public static final String FOLDER_ID = "folderId";
    public static final String OWNER_ID = "ownerId";
    public static final String SENDER_ID = "senderId";
    public static final String RECIPIENT_ID = "recipientId";
    public static final String SUBJECT = "subject";
    public static final String SENT = "sent";
    public static final String UNREAD = "unread";
    public static final String MESSAGE = "message";
    
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
    @Column(nullable=false)
    public int senderId;

    /** The id of the recipient. */
    @Column(nullable=false)
    public int recipientId;

    /** The subject of this message. */
    @Column(nullable=false)
    public String subject;
    
    /** The time at which the message was delivered. */
    @Column(nullable=false)
    public Timestamp sent;

    /** Whether or not this message is yet to be read. */
    @Column(nullable=false)
    public boolean unread;
    
    /** The actual message text. */
    @Column(length=32768)
    public String message;

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
