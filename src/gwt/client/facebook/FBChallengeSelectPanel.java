//
// $Id$

package client.facebook;

import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.util.Link;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookServiceAsync;
import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

/**
 * Just shows 3 buttons that direct to different target audiences.
 */
public class FBChallengeSelectPanel extends FlowPanel
{
    public FBChallengeSelectPanel (Args args, String gameName)
    {
        setStyleName("challengeSelect");
        add(MsoyUI.createLabel(_msgs.challengeSelect(gameName), "Title"));
        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        buttons.setStyleName("Buttons");
        buttons.add(easyButton(_msgs.challengeAllFriendsBtn(), "AllFriends",
            Link.createHandler(Pages.FACEBOOK, args, ArgNames.FB_CHALLENGE_FRIENDS)));
        buttons.add(easyButton(_msgs.challengeAppFriendsBtn(), "AppFriends",
            Link.createHandler(Pages.FACEBOOK, args, ArgNames.FB_CHALLENGE_APP_FRIENDS)));
        buttons.add(easyButton(_msgs.challengeSomeFriendsBtn(), "SomeFriends",
            Link.createHandler(Pages.FACEBOOK, args, ArgNames.FB_CHALLENGE_PICK)));
        add(buttons);
    }

    protected PushButton easyButton (String text, String style, ClickHandler handler)
    {
        PushButton button = MsoyUI.createImageButton("easyButton", handler);
        button.setText(text);
        button.addStyleName(style);
        return button;
    }

    protected static final FacebookMessages _msgs = GWT.create(FacebookMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
}
