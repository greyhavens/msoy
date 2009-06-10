//
// $Id$

package client.landing;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.SimplePanel;

import client.ui.MsoyUI;
import client.util.NaviUtil;

/**
 * Displays a summary of what Whirled is aimed at people who like to create things.  Whirleds,
 * rooms, avatars, furniture and decors highlighted, no mention of games.
 */
public class CreatorsPanel extends SimplePanel
{
    public CreatorsPanel ()
    {
        setStyleName("creatorsPanel");
        AbsolutePanel content = new AbsolutePanel();
        content.setStyleName("Content");
        setWidget(content);
        content.add(MsoyUI.createImageButton("GetStartedButton", NaviUtil.onSignUp()), 342, 381);
        content.add(MsoyUI.createActionImage("/images/landing/creators_info.jpg",
                                             NaviUtil.onSignUp()), 15, 504);
        content.add(new LandingCopyright(), 0, 1085);
    }
}
