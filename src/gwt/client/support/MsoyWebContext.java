//
// $Id$

package client.support;

import com.threerings.msoy.underwire.gwt.SupportServiceAsync;
import com.threerings.underwire.gwt.client.WebContext;

/**
 * Extends the underwire context for support pages to contain some msoy bits that we are likely to
 * need.
 */
public class MsoyWebContext extends WebContext
{
    /** The service for updating msoy support data. */
    public SupportServiceAsync supportService;
}
