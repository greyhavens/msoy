//
// $Id$

package client.landing;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;

import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays a summary of what Whirled is aimed at people who like to create things.
 * Whirleds, rooms, avatars, furniture and decors highlighted, no mention of games.
 */
public class CreatorsLinksPanel extends SimplePanel
{
    public CreatorsLinksPanel ()
    {
        setStyleName("creatorsPanel");
        AbsolutePanel content = new AbsolutePanel();
        content.setStyleName("Content");
        setWidget(content);

        PushButton getStartedButton = new PushButton();
        getStartedButton.setStyleName("GetStartedButton");
        getStartedButton.addClickListener(Link.createListener(Pages.ACCOUNT, "create"));
        getStartedButton.addClickListener(
            MsoyUI.createTrackingListener("creatorsLinksGetStartedClicked", null));
        content.add(getStartedButton, 342, 381);

        Image furniImage = MsoyUI.createActionImage("/images/landing/creators_info_furni.jpg",
            Link.createListener(Pages.SHOP, "3"));
        furniImage.addClickListener(
            MsoyUI.createTrackingListener("creatorsLinksFurniClicked", null));
        content.add(furniImage, 7, 502);

        Image avatarsImage = MsoyUI.createActionImage("/images/landing/creators_info_avatars.jpg",
            Link.createListener(Pages.SHOP, "5"));
        avatarsImage.addClickListener(
            MsoyUI.createTrackingListener("creatorsLinksAvatarsClicked", null));
        content.add(avatarsImage, 311, 502);

        Image whirledsImage =
            MsoyUI.createActionImage("/images/landing/creators_info_whirleds.jpg",
            Link.createListener(Pages.WHIRLEDS, ""));
        whirledsImage.addClickListener(
            MsoyUI.createTrackingListener("creatorsLinksWhirledsClicked", null));
        content.add(whirledsImage, 7, 751);

        Image friendsImage = MsoyUI.createActionImage("/images/landing/creators_info_friends.jpg",
            Link.createListener(Pages.WHIRLEDS, ""));
        friendsImage.addClickListener(
            MsoyUI.createTrackingListener("creatorsLinksFriendsClicked", null));
        content.add(friendsImage, 311, 751);

        content.add(new LandingCopyright(), 0, 1085);
    }
}
