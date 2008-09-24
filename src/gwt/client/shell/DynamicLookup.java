//
// $Id$

package client.shell;

import com.threerings.gwt.util.MessagesLookup;

/**
 * Provides dynamic translation message lookup for things that need it.
 */
@MessagesLookup.Lookup(using="client.shell.DynamicMessages")
public abstract class DynamicLookup extends MessagesLookup
{
}
