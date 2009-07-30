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
 * TODO: name of the inviter in the invitation?
 * TODO: hyperlinked stuff in the invitation
 * TODO: "not right now button" to workaround the "Ignore" button lowering our allocations
 */
public class FBInvitePanel extends ServerFBMLPanel
{
    /**
     * Creates a panel to contain the generic invite, then populates it once the user's non-whirled
     * friends are known.
     */
    public static Widget createGeneric ()
    {
        // TOOD: some loading message
        final FlowPanel div = new FlowPanel();
        _fbsvc.getFriendsUsingApp(new InfoCallback<List<Long>>() {
            public void onSuccess (List<Long> result) {
                String app = DeploymentConfig.facebookApplicationName;
                div.add(new FBInvitePanel(result, _msgs.inviteGeneric(app),
                    _msgs.inviteGenericTip(), _msgs.inviteGenericAccept(app), "", app));
            }
        });
        return div;
    }

    /**
     * Creates a panel to contain the challenge request, then populates it once the game name is
     * known.
     * TODO: high scores?
     */
    public static Widget createChallenge (final int gameId)
    {
        // TOOD: some loading message
        final FlowPanel div = new FlowPanel();
        _fbsvc.getGameName(gameId, new InfoCallback<String>() {
            String app = DeploymentConfig.facebookApplicationName;
            public void onSuccess (String result) {
                div.add(new FBInvitePanel(null, _msgs.inviteChallenge(app, result),
                    _msgs.inviteChallengeTip(), _msgs.inviteChallengeAccept(result),
                    "game=" + gameId, result));
            }
        });
        return div;
    }

    /**
     * Creates a new invite request form that excludes the given friend ids and has the given
     * translation strings.
     * @param text the invitation copy, such as "Come Play With Me"
     * @param tip mini-instructions at the top of the form, such "Select some friends to play"
     * @param accept the copy on the accept button of the invitation
     * @param canvasArgs "name1=value1&name2=value2" arguments to add to the accept button url
     * @param type the type of invitation shown in the form's "send" button
     */
    protected FBInvitePanel (List<Long> excludeIds, String text, String tip, String accept,
        String canvasArgs, String type)
    {
        StringBuilder exclude = new StringBuilder();
        if (excludeIds != null) {
            for (Long id : excludeIds) {
                exclude.append(id).append(",");
            }
        }
        if (canvasArgs.length() > 0) {
            canvasArgs += "&";
        }
        canvasArgs += CookieNames.AFFILIATE + "=" + CShell.getMemberId();
        FBMLPanel form = new FBMLPanel("request-form",
            // TODO: give the fbinvite servlet enough information to go back to where we were
            "action", DeploymentConfig.serverURL + "fbinvite/ndone",
            "method", "POST",
            "invite", "true",
            // Facebook ignores escapes in here, sanitize instead
            "type", StringUtil.sanitizeAttribute(type),
            "content", makeContent(text, accept, canvasArgs));
        form.add(new FBMLPanel("multi-friend-selector",
            "showborder", String.valueOf(false),
            "rows", String.valueOf(6),
            "actiontext", tip,
            "bypass", "cancel",
            "exclude_ids", exclude.toString()));
        add(form);
    }

    protected static String makeContent (String text, String accept, String canvasArgs)
    {
        String url = DeploymentConfig.facebookCanvasUrl + "?" + canvasArgs;
        // weird... the req-choice tag goes inside the content attribute
        FBMLPanel reqChoice = new FBMLPanel("req-choice", "url", url, "label", accept);
        FlowPanel div = new FlowPanel();
        div.add(reqChoice);
        return StringUtil.escapeAttribute(text + div.getElement().getInnerHTML());
    }

    protected static final FacebookMessages _msgs = GWT.create(FacebookMessages.class);
    protected static final FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
}
