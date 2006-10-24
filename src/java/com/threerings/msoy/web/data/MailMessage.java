//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Represents a full message, both the metadata and the actual message text.
 */

public class MailMessage
    implements IsSerializable, Streamable
{
    /** All the metadata for this message. */
    public MailHeaders headers;

    /** The actual message text. */
    public String message;
}
