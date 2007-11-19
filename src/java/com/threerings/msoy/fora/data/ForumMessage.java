//
// $Id$

package com.threerings.msoy.fora.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.web.data.MemberCard;

/**
 * Contains information on a forum message.
 */
public class ForumMessage
    implements IsSerializable
{
    /** This message's unique identifier. */
    public int messageId;

    /** The id of the thread to which this message belongs. */
    public int threadId;

    /** The id of the message to which this message is a reply, or zero. */
    public int inReplyTo;

    /** The name and profile photo of the member that posted this message. */
    public MemberCard poster;

    /** The time at which this message was created. */
    public Date created;

    /** The time at which this message was last edited. */
    public Date lastEdited;

    /** The text of this message. */
    public String message;
}
