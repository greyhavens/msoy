//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;

/**
 * Displays some reasons to play games.
 */
public class WhyPlayPanel extends FlowPanel
{
    public WhyPlayPanel ()
    {
        setStyleName("whyPlay");
        add(new HTML(CGames.msgs.whyPlayBlurb()));
    }
}
