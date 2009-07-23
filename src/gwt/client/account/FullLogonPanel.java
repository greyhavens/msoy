//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasAlignment;

import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.FacebookCreds;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;
import client.shell.LogonPanel;
import client.ui.MsoyUI;
import client.util.InfoCallback;

/**
 * Extends the simple logon panel with external authentication stuffs. This version can only be
 * used in the "account" section of the site as it relies on a bunch of JavaScript wirey uppy that
 * we only make work in that section.
 */
public class FullLogonPanel extends LogonPanel
{
    public FullLogonPanel ()
    {
        super(Mode.HORIZ, MsoyUI.createButton(MsoyUI.MEDIUM_THIN, _msgs.logonLogon(), null));

        // add the interface for logging in via Facebook connect
        setText(1, 0, _msgs.logonFacebook(), 1);
        getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_RIGHT);
        setWidget(1, 1, MsoyUI.createActionImage(FBCON_IMG, new ClickHandler() {
            public void onClick (ClickEvent event) {
                // TODO: display a little circular "pending" icon; turn off clickability
                initiateFacebookLogon();
            }
        }), 1);
    }

    protected void initiateFacebookLogon ()
    {
        _fbconnect.requireSession(new InfoCallback<String>() {
            public void onSuccess (String uid) {
                CShell.log("Got Facebook Connect uid '" + uid + "'.");
                FacebookCreds creds = FBConnect.readCreds();
                if (creds == null) {
                    MsoyUI.error("Unable to connect to Facebook. Sorry!"); // TODO
                    return;
                }
                // TODO: send permaguest member id here
                _usersvc.externalLogon(
                    DeploymentConfig.version, creds, CShell.frame.getVisitorInfo(),
                    WebUserService.SESSION_DAYS, new InfoCallback<SessionData>() {
                    public void onSuccess (SessionData data) {
                        CShell.frame.dispatchDidLogon(data);
                    }
                });
            }
        });
    }

    protected FBConnect _fbconnect = new FBConnect();

    protected static final AccountMessages _msgs = GWT.create(AccountMessages.class);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);

    protected static final String FBCON_IMG = "/images/account/fbconnect.png";
}
