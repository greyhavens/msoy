//
// $Id$

package com.threerings.msoy.mail.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.mail.gwt.ConvMessage;
import com.threerings.msoy.mail.gwt.MailPayload;
import com.threerings.msoy.server.util.JSONMarshaller;

import static com.threerings.msoy.Log.log;

/**
 * Contains a single message in a conversation.
 */
@Entity
public class ConvMessageRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #conversationId} field. */
    public static final String CONVERSATION_ID = "conversationId";

    /** The qualified column identifier for the {@link #conversationId} field. */
    public static final ColumnExp CONVERSATION_ID_C =
        new ColumnExp(ConvMessageRecord.class, CONVERSATION_ID);

    /** The column identifier for the {@link #sent} field. */
    public static final String SENT = "sent";

    /** The qualified column identifier for the {@link #sent} field. */
    public static final ColumnExp SENT_C =
        new ColumnExp(ConvMessageRecord.class, SENT);

    /** The column identifier for the {@link #authorId} field. */
    public static final String AUTHOR_ID = "authorId";

    /** The qualified column identifier for the {@link #authorId} field. */
    public static final ColumnExp AUTHOR_ID_C =
        new ColumnExp(ConvMessageRecord.class, AUTHOR_ID);

    /** The column identifier for the {@link #body} field. */
    public static final String BODY = "body";

    /** The qualified column identifier for the {@link #body} field. */
    public static final ColumnExp BODY_C =
        new ColumnExp(ConvMessageRecord.class, BODY);

    /** The column identifier for the {@link #payloadType} field. */
    public static final String PAYLOAD_TYPE = "payloadType";

    /** The qualified column identifier for the {@link #payloadType} field. */
    public static final ColumnExp PAYLOAD_TYPE_C =
        new ColumnExp(ConvMessageRecord.class, PAYLOAD_TYPE);

    /** The column identifier for the {@link #payloadState} field. */
    public static final String PAYLOAD_STATE = "payloadState";

    /** The qualified column identifier for the {@link #payloadState} field. */
    public static final ColumnExp PAYLOAD_STATE_C =
        new ColumnExp(ConvMessageRecord.class, PAYLOAD_STATE);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The conversation of which this message is a part. */
    @Id public int conversationId;

    /** The time at which the message was delivered. */
    @Id public Timestamp sent;

    /** The member id of the author of this message. */
    public int authorId;

    /** The text part of the message body. */
    @Column(length=32768)
    public String body;

    /** An integer specifying which type of {@link MailPayload} object this message includes. */
    public int payloadType;

    /** The low-level representation of the state of a {@link MailPayload} object. */
    @Column(length=16384, nullable=true)
    public byte[] payloadState;

    /**
     * Converts this persistent record to a runtime record. The {@link ConvMessage#author} is not
     * filled in.
     */
    public ConvMessage toConvMessage ()
    {
        ConvMessage cmsg = new ConvMessage();
        cmsg.sent = new Date(sent.getTime());
        cmsg.body = body;

        if (payloadType != 0 && payloadState != null) {
            try {
                Class<? extends MailPayload> objectClass = MailPayload.getPayloadClass(payloadType);
                JSONMarshaller<? extends MailPayload> marsh =
                    JSONMarshaller.getMarshaller(objectClass);
                cmsg.payload = marsh.newInstance(payloadState);
            } catch (Exception e) {
                log.warning("Failed to unserialize message payload [tpye=" + payloadType +
                        ", state=" + StringUtil.hexlate(payloadState) + "].", e);
            }
        }

        return cmsg;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #ConvMessageRecord}
     * with the supplied key values.
     */
    public static Key<ConvMessageRecord> getKey (int conversationId, Timestamp sent)
    {
        return new Key<ConvMessageRecord>(
                ConvMessageRecord.class,
                new String[] { CONVERSATION_ID, SENT },
                new Comparable[] { conversationId, sent });
    }
    // AUTO-GENERATED: METHODS END
}
