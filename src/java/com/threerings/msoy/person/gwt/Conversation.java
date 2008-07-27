//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.web.data.MemberCard;

/**
 * Contains information on a mail conversation.
 */
public class Conversation
    implements IsSerializable
{
    /** The length of our last message snippet. */
    public static final int SNIPPET_LENGTH = 80;

    /** The unique identifier of this conversation. */
    public int conversationId;

    /** The other party in this conversation. */
    public MemberCard other;

    /** The subject of this conversation. */
    public String subject;

    /** The time at which the most recent message was sent in this conversation. */
    public Date lastSent;

    /** The id of the last author in this conversation. */
    public int lastAuthorId;

    /** A snippet of the text of the most recent message. */
    public String lastSnippet;

    /** Whether or not this conversation has unread messages. */
    public boolean hasUnread;
}
