//
// $Id$

package client.landing;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;

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

        ClickHandler onStart = Link.createHandler(Pages.ACCOUNT, "create");
        content.add(MsoyUI.createImageButton("GetStartedButton", onStart), 342, 381);

        ClickHandler onInfo = Link.createHandler(Pages.ACCOUNT, "create");
        content.add(MsoyUI.createActionImage("/images/landing/creators_info.jpg", onInfo), 15, 504);

        content.add(new LandingCopyright(), 0, 1085);
    }
}
