//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;

import client.ui.MsoyUI;

/**
 * Displays some reasons to play games.
 */
public class WhyPlayPanel extends FlowPanel
{
    public WhyPlayPanel ()
    {
        setStyleName("whyPlay");
        add(MsoyUI.createLabel(CGames.msgs.whyPlayTitle(), "Title"));
        add(new HTML(CGames.msgs.whyPlayBlurb()));
    }
}
