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
    
    // @Override
    public int hashCode ()
    {
        return headers.hashCode() + 31*headers.hashCode();
    }

    // @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MailMessage other = (MailMessage) obj;
        if (headers == null) {
            return other.headers == null;
        }
        if (message == null) {
            return other.message == null;
        }
        return headers.equals(other.headers) && message.equals(other.message);
    }
}
