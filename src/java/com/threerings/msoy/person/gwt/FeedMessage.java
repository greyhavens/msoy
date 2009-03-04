//
// $Id$

package com.threerings.msoy.person.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a feed message.
 */
public class FeedMessage
    implements IsSerializable
{
    /** The type of feed message. */
    public FeedMessageType type;

    /** The arguments to this feed message. */
    public String[] data;

    /** The time at which this message was posted. */
    public long posted;
}
