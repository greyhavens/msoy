package client.shell;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.FacebookCreds;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.ui.MsoyUI;
import client.util.InfoCallback;

/**
 * Implement Facebook Connect authentication. Any GWT app  that wishes to use it should include
 * some special JavaScript wiring: the app index.html must include the fbhelper script as well as
 * the remote FB API code. See src/gwt/public/account/index.html.
 */
public class FBLogonPanel extends FlowPanel
{
    public FBLogonPanel ()
    {
        this("/images/account/fbconnect.png");
    }

    public FBLogonPanel (String img)
    {
        this.add(MsoyUI.createActionImage(img, new ClickHandler() {
            public void onClick (ClickEvent event) {
                // TODO: display a little circular "pending" icon; turn off clickability
                initiateFacebookLogon();
            }
        }));
    }

    protected void initiateFacebookLogon ()
    {
        _fbconnect.requireSession(new InfoCallback<String>() {
            public void onSuccess (String uid) {
                FacebookCreds creds = FBConnect.readCreds();
                if (creds == null) {
                    MsoyUI.error("Unable to connect to Facebook. Sorry!"); // TODO
                    return;
                }
                // TODO: send permaguest member id here
                _usersvc.externalLogon(
                    DeploymentConfig.version, creds, CShell.frame.getVisitorInfo(),
                    WebUserService.SESSION_DAYS, CShell.getAppId(),
                    new InfoCallback<SessionData>() {
                    public void onSuccess (SessionData data) {
                        CShell.frame.dispatchDidLogon(data);
                    }
                });
            }
        });
    }

    protected FBConnect _fbconnect = new FBConnect();

    protected static final WebUserServiceAsync _usersvc = GWT.create(WebUserService.class);
}
