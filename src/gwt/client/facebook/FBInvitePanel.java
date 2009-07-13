//
// $Id$

package client.facebook;

import java.util.List;

import client.shell.CShell;
import client.util.InfoCallback;
import client.util.StringUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookServiceAsync;
import com.threerings.msoy.web.gwt.CookieNames;

/**
 * Server FBML panel for inviting friends. Currently only deals with site invitations.
 * TODO: game invitations
 */
public class FBInvitePanel extends ServerFBMLPanel
{
    public static Widget create ()
    {
        // TOOD: some loading message
        final FlowPanel div = new FlowPanel();
        _fbsvc.getFriendsUsingApp(new InfoCallback<List<Long>>() {
            public void onSuccess (List<Long> result) {
                div.add(new FBInvitePanel(result));
            }
        });
        return div;
    }

    /**
     * Creates a new invite panel.
     */
    protected FBInvitePanel (List<Long> excludeIds)
    {
        StringBuilder exclude = new StringBuilder();
        for (Long id : excludeIds) {
            exclude.append(id).append(",");
        }
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
            "rows", String.valueOf(6),
            "actiontext", _msgs.inviteGenericTip(),
            "bypass", "cancel",
            "exclude_ids", exclude.toString()));
        add(form);
    }

    protected static String makeContent ()
    {
        String url = DeploymentConfig.facebookCanvasUrl + "?" + CookieNames.AFFILIATE + "=" +
            CShell.getMemberId();
        // weird... the req-choice tag goes inside the content attribute
        FBMLPanel reqChoice = new FBMLPanel("req-choice", "url", url,
            "label", _msgs.inviteGenericAccept(DeploymentConfig.facebookApplicationName));
        FlowPanel div = new FlowPanel();
        div.add(reqChoice);
        return StringUtil.escapeAttribute(
            _msgs.inviteGeneric(DeploymentConfig.facebookApplicationName) +
            div.getElement().getInnerHTML());
    }

    protected static final FacebookMessages _msgs = GWT.create(FacebookMessages.class);
    protected static final FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
}
