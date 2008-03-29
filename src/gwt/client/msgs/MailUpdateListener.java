//
// $Id$

package client.msgs;

import com.threerings.msoy.person.data.ConvMessage;

/**
 * Used by an object to notify another object that a mail message has changed, and will need
 * refreshing from the database before it can be considered recent again.
 */
public interface MailUpdateListener
{
    public void messageChanged (int convoId, ConvMessage message);
}
