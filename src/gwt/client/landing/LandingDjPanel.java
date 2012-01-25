//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;

import client.shell.FBConnect;
import client.shell.FBLogonPanel;
import client.shell.LogonPanel;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.RegisterPanel;
import client.util.InfoCallback;

public class LandingDjPanel extends SimplePanel
{
    public LandingDjPanel (boolean light)
    {
        String scheme = light ? "light" : "dark";
        setStyleName("landingDj " + scheme);
        FlowPanel content = MsoyUI.createFlowPanel("Content");
        this.add(content);

        // logo
        content.add(MsoyUI.createImage("/images/landing/monsterave/logo_big_white.png", "Logo"));

        // login box
        FBLogonPanel fbLogon = new FBLogonPanel(
            "/images/account/fbconnect_big.png", "/images/ui/bar_loader.gif");
        fbLogon.setStyleName("Connect");
        content.add(MsoyUI.createHTML(_msgs.djFooter(), "Info"));
        content.add(fbLogon);

        // text and copyright
        FlowPanel footer = MsoyUI.createFlowPanel("Footer");
        footer.add(LandingCopyright.addFinePrint(MsoyUI.createFlowPanel("Copyright")));
        content.add(footer);
    }

    protected FlowPanel _registerPlaceholder;
    protected RegisterPanel _register;

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
