//
// $Id$

package com.threerings.msoy.person.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.person.util.FeedMessageType;

/**
 * Contains information on a feed message.
 */
public class FeedMessage
    implements IsSerializable
{
    /** The numeric code indicating the type of this message. See {@link FeedMessageType}. */
    public int type;

    /** The arguments to this feed message. */
    public String[] data;

    /** The time at which this message was posted. */
    public long posted;
}
