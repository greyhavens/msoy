//
// $Id$

package com.threerings.msoy.mail.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Records a complaint registered by a user about a conversation.
 */
@Entity
public class ConversationComplaintRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ConversationComplaintRecord> _R = ConversationComplaintRecord.class;
    public static final ColumnExp<Integer> CONVERSATION_ID = colexp(_R, "conversationId");
    public static final ColumnExp<Integer> COMPLAINER_ID = colexp(_R, "complainerId");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The conversation in question. */
    @Id public int conversationId;

    /** The id of the member that complained. */
    @Id public int complainerId;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ConversationComplaintRecord}
     * with the supplied key values.
     */
    public static Key<ConversationComplaintRecord> getKey (int conversationId, int complainerId)
    {
        return newKey(_R, conversationId, complainerId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(CONVERSATION_ID, COMPLAINER_ID); }
    // AUTO-GENERATED: METHODS END
}
