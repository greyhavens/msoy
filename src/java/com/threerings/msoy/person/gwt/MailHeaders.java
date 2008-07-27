//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;
import com.threerings.msoy.data.all.MemberName;

/**
 * Represents all the metadata for a mail message.
 */
public final class MailHeaders
    implements IsSerializable, Streamable
{
    /** The id of this message, unique within its current folder. */
    public int messageId;

    /** The id of the folder in which this message is currently filed. */
    public int folderId;

    /** The message sender. */
    public MemberName sender;

    /** The message recipient. */
    public MemberName recipient;

    /** The subject of this message. */
    public String subject;

    /** The time at which the message was delivered. */
    public Date sent;

    /** Whether or not this message has yet to be read. */
    public boolean unread;

    /**
     * Returns the member name of the "other" party in this message.
     */
    public MemberName getOther (int myMemberId)
    {
        return (sender.getMemberId() == myMemberId) ? recipient : sender;
    }

    @Override
    public int hashCode ()
    {
        return messageId + 31*folderId;
    }

    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof MailHeaders)) {
            return false;
        }
        MailHeaders other = (MailHeaders) obj;
        return (other.messageId == messageId && other.folderId == folderId);
    }
}
