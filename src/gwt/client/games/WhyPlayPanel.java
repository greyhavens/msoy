//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import client.ui.MsoyUI;

/**
 * Displays some reasons to play games.
 */
public class WhyPlayPanel extends FlowPanel
{
    public WhyPlayPanel ()
    {
        setStyleName("whyPlay");
        add(MsoyUI.createLabel(_msgs.whyPlayTitle(), "Title"));
        add(MsoyUI.createHTML(_msgs.whyPlayBlurb(), null));
    }

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
}
