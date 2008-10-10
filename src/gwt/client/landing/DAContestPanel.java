//
// $Id$

package client.landing;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * Landing page for the Deviant Art "Design Your Whirled" contest.
 */
public class DAContestPanel extends SimplePanel
{
    public DAContestPanel ()
    {
        setStyleName("daContestPanel");
        AbsolutePanel content = new AbsolutePanel();
        content.setStyleName("Content");
        setWidget(content);

        content.add(new Image("/images/landing/da_contest_mockup.jpg"), 0, 0);
    }
}
