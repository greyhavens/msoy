//
// $Id$

package com.threerings.msoy.mail.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.samskivert.util.StringUtil;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.mail.gwt.ConvMessage;
import com.threerings.msoy.mail.gwt.FriendInvitePayload;
import com.threerings.msoy.mail.gwt.GameInvitePayload;
import com.threerings.msoy.mail.gwt.GroupInvitePayload;
import com.threerings.msoy.mail.gwt.MailPayload;
import com.threerings.msoy.mail.gwt.PresentPayload;
import com.threerings.msoy.mail.gwt.RoomGiftPayload;
import com.threerings.msoy.server.util.JSONMarshaller;

import static com.threerings.msoy.Log.log;

/**
 * Contains a single message in a conversation.
 */
@Entity
public class ConvMessageRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ConvMessageRecord> _R = ConvMessageRecord.class;
    public static final ColumnExp<Integer> CONVERSATION_ID = colexp(_R, "conversationId");
    public static final ColumnExp<Timestamp> SENT = colexp(_R, "sent");
    public static final ColumnExp<Integer> AUTHOR_ID = colexp(_R, "authorId");
    public static final ColumnExp<String> BODY = colexp(_R, "body");
    public static final ColumnExp<Integer> PAYLOAD_TYPE = colexp(_R, "payloadType");
    public static final ColumnExp<byte[]> PAYLOAD_STATE = colexp(_R, "payloadState");
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
        cmsg.conversationId = conversationId;
        cmsg.sent = new Date(sent.getTime());
        cmsg.body = body;

        if (payloadType != 0 && payloadState != null) {
            try {
                Class<? extends MailPayload> objectClass = getPayloadClass(payloadType);
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

    /**
     * Returns the class registered for the specified payload type.
     *
     * @exception IllegalArgumentException thrown if an unknown payload type is provided.
     */
    protected static Class<? extends MailPayload> getPayloadClass (int type)
    {
        switch (type) {
        case MailPayload.TYPE_GROUP_INVITE:
            return GroupInvitePayload.class;
        case MailPayload.TYPE_FRIEND_INVITE:
            return FriendInvitePayload.class;
        case MailPayload.TYPE_PRESENT:
            return PresentPayload.class;
        case MailPayload.TYPE_GAME_INVITE:
            return GameInvitePayload.class;
        case MailPayload.TYPE_ROOM_GIFT:
            return RoomGiftPayload.class;
        }
        throw new IllegalArgumentException("Unknown payload [type= " + type + "]");
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ConvMessageRecord}
     * with the supplied key values.
     */
    public static Key<ConvMessageRecord> getKey (int conversationId, Timestamp sent)
    {
        return newKey(_R, conversationId, sent);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(CONVERSATION_ID, SENT); }
    // AUTO-GENERATED: METHODS END
}
