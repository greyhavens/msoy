//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hyperlink;

import com.threerings.gwt.ui.WidgetUtil;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MsoyUI;

/**
 * Displays some reasons to play games.
 */
public class WhyPlayPanel extends FlowPanel
{
    public WhyPlayPanel ()
    {
        setStyleName("whyPlay");

        add(new HTML(CGame.msgs.whyPlayBlurb()));

        if (CGame.getMemberId() != 0) {
            add(WidgetUtil.makeShim(10, 10));
            Hyperlink link = Application.createLink(
                "View your trophies", Page.GAMES, Args.compose("t", CGame.getMemberId()));
            link.addStyleName("inline");
            add(link);
        }
    }
}
