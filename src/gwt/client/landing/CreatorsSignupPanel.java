package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.SimplePanel;

import client.account.CreateAccountPanel;
import client.ui.MsoyUI;

/**
 * Displays a summary of what Whirled is aimed at people who like to create things, then
 * a registration form.
 */
public class CreatorsSignupPanel extends SimplePanel
{
    public CreatorsSignupPanel ()
    {
        setStyleName("creatorsSignupPanel");
        AbsolutePanel content = new AbsolutePanel();
        content.setStyleName("Content");
        setWidget(content);

        content.add(MsoyUI.createHTML(_msgs.creatorsGetStarted(), "GetStarted"), 377, 470);
        content.add(new CreateAccountPanel(), 10, 530);
        content.add(new LandingCopyright(), 0, 1085);
    }

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
}
