//
// $Id$

package client.frame;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.HistoryListener;

/**
 * Handles the outer shell of the Whirled web application. Loads pages into an iframe and also
 * handles displaying the Flash client.
 */
public class FrameEntryPoint
    implements EntryPoint, HistoryListener
{
    // from interface EntryPoint
    public void onModuleLoad ()
    {
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
    }
}
