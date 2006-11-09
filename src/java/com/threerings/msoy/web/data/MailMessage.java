//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Represents a full message, both the metadata and the actual message text.
 */

final public class MailMessage
    implements IsSerializable, Streamable
{
    /** All the metadata for this message. */
    public MailHeaders headers;

    /** The (optional) text part of the message body. */
    public String bodyText;
    
    /** The (optional) object part of the message body. */
    public MailBodyObject bodyObject;
    
    // @Override
    public int hashCode ()
    {
        int hashCode = headers.hashCode();
        if (bodyText != null) {
            hashCode = 31*hashCode + bodyText.hashCode();
        }
        if (bodyObject != null) {
            hashCode = 31*hashCode + bodyObject.hashCode();
        }
        return hashCode;
    }


    // @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof MailMessage)) {
            return false;
        }
        MailMessage other = (MailMessage) obj;
        if (headers == null) {
            if (other.headers != null) {
                return false;
            }
        } else if (!headers.equals(other.headers)) {
            return false;
        }
        if (bodyObject == null) {
            if (other.bodyObject != null) {
                return false;
            }
        } else if (!bodyObject.equals(other.bodyObject)) {
            return false;
        }
        if (bodyText == null) {
            return other.bodyText == null;
        }
        return bodyText.equals(other.bodyText);
    }
}
