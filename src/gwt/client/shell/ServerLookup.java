//
// $Id$

package client.shell;

import com.threerings.gwt.util.MessagesLookup;

/**
 * Provides dynamic translation lookup for messages coming back from the server.
 */
@MessagesLookup.Lookup(using="client.shell.ServerMessages")
public abstract class ServerLookup extends MessagesLookup
{
}
