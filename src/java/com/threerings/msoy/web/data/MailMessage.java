//
// $Id$

package com.threerings.msoy.web.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Represents a message, with some meta-data, in a folder belonging to a member.
 */

public class MailMessage
    implements IsSerializable, Streamable
{
    /** The id of this message, unique within its current folder. */
    public int messageId;

    /** The id of the folder in which this message is currently filed. */
    public int folderId;

    /** The id of the owner of this message. */
    public int ownerId;

    /** The message sender. */
    public MemberGName sender;

    /** The message recipient. */
    public MemberGName recipient;

    /** The subject of this message. */
    public String subject;
    
    /** The time at which the message was delivered. */
    public Date sent;

    /** The actual message text. */
    public String message;
}
