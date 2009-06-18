//
// $Id$

package client.facebook;

import client.util.StringUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.data.all.DeploymentConfig;

/**
 * Server FBML panel for inviting friends. Currently only deals with site invitations.
 * TODO: game invitations
 */
public class FBInvitePanel extends ServerFBMLPanel
{
    /**
     * Creates a new invite panel.
     */
    public FBInvitePanel ()
    {
        FBMLPanel form = new FBMLPanel("request-form",
            // TODO: give the fbinvite servlet enough information to go back to where we were
            "action", DeploymentConfig.serverURL + "fbinvite/ndone",
            "method", "POST",
            "invite", "true",
            // Facebook ignores escapes in here, sanitize instead 
            "type", StringUtil.sanitizeAttribute(DeploymentConfig.facebookApplicationName),
            "content", makeContent());
        form.add(new FBMLPanel("multi-friend-selector",
            "showborder", String.valueOf(false),
            "rows", String.valueOf(5),
            "actiontext", _msgs.inviteGenericTip(),
            "bypass", "cancel"));
        add(form);
    }

    protected static String makeContent ()
    {
        // weird... the req-choice tag goes inside the content attribute
        FBMLPanel reqChoice = new FBMLPanel("req-choice",
            "url", DeploymentConfig.facebookCanvasUrl,
            "label", _msgs.inviteGenericAccept(DeploymentConfig.facebookApplicationName));
        FlowPanel div = new FlowPanel();
        div.add(reqChoice);
        return StringUtil.escapeAttribute(
            _msgs.inviteGeneric(DeploymentConfig.facebookApplicationName) +
            div.getElement().getInnerHTML());
    }

    protected static final FacebookMessages _msgs = GWT.create(FacebookMessages.class);
}
