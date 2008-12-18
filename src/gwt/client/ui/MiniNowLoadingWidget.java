//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * Small widget with a now loading indicator, useful for small panels waiting for something to load
 * from the server.
 * 
 * TODO: finishing() method similar to NowLoadingWidget
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class MiniNowLoadingWidget extends SimplePanel
{
    public MiniNowLoadingWidget ()
    {
        setWidget(new Image("/images/ui/dot_loader.gif"));
    }
}
