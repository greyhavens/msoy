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
import com.threerings.msoy.facebook.gwt.FacebookService.InviteInfo;
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
     * Creates a panel to contain a generic invite, automatically populating it once the
     * appropriate invite information is returned.
     */
    public static Widget createGeneric ()
    {
        // TOOD: some loading message
        final FlowPanel div = new FlowPanel();
        _fbsvc.getInviteInfo("", new InfoCallback<InviteInfo>() {
            public void onSuccess (InviteInfo result) {
                // inviteGeneric = {0} has invited you to join {1} team on {2}.
                String app = DeploymentConfig.facebookApplicationName;
                String invite = _msgs.inviteGeneric(
                    result.username, getPronoun(result.gender), app);
                String tip = _msgs.inviteGenericTip();
                String accept = _msgs.inviteChallengeAccept(app);
                div.add(new FBInvitePanel(result.excludeIds, invite, tip, accept, "", app));
            }
        });
        return div;
    }

    /**
     * Creates a panel to contain a challenge request for the given whirled game id, automatically
     * populating it once the appropriate invite information is returned.
     * TODO: high scores?
     */
    public static Widget createChallenge (InviteInfo info, int gameId)
    {
        // TOOD: some loading message
        final FlowPanel div = new FlowPanel();
        // {0} just played {1} on {2} and challenges you to beat {3} high score!
        String app = DeploymentConfig.facebookApplicationName;
        String invite = _msgs.inviteChallenge(
            info.username, info.gameName, app, getPronoun(info.gender));
        String tip = _msgs.inviteChallengeTip();
        String accept = _msgs.inviteChallengeAccept(info.gameName);
        div.add(new FBInvitePanel(
            info.excludeIds, invite, tip, accept, "game=" + gameId, info.gameName));
        return div;
    }

    /**
     * Creates a panel to contain a challenge request for the given mopchi game tag, automatically
     * populating it once the appropriate invite information is returned.
     */
    public static Widget createMochiChallenge (InviteInfo info, String mochiTag)
    {
        // TOOD: some loading message
        final FlowPanel div = new FlowPanel();
        // {0} just played {1} on {2} and challenges you to beat {3} high score!
        String app = DeploymentConfig.facebookApplicationName;
        String invite = _msgs.inviteChallenge(
            info.username, info.gameName, app, getPronoun(info.gender));
        String tip = _msgs.inviteChallengeTip();
        String accept = _msgs.inviteChallengeAccept(info.gameName);
        div.add(new FBInvitePanel(
            info.excludeIds, invite, tip, accept, "mgame=" + mochiTag, info.gameName));
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
        return StringUtil.escapeAttribute(text) + div.getElement().getInnerHTML();
    }

    protected static String getPronoun (FacebookService.Gender gender)
    {
        switch (gender) {
        case FEMALE: return _msgs.possessiveHers();
        case MALE: return _msgs.possessiveHis();
        case HIDDEN: return _msgs.possessiveNeutral();
        }
        return "";
    }

    protected static final FacebookMessages _msgs = GWT.create(FacebookMessages.class);
    protected static final FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
}
