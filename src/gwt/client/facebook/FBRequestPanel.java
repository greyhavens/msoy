//
// $Id$

package client.facebook;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.facebook.gwt.FacebookGame;
import com.threerings.msoy.facebook.gwt.FacebookService.InviteInfo;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookServiceAsync;
import com.threerings.msoy.web.gwt.ArgNames.FBParam;
import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.SharedNaviUtil;

import client.facebookbase.FacebookUtil;
import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.ArrayUtil;
import client.util.InfoCallback;

/**
 * Server FBML panel for sending requests to friends. Currently deals with site invitations and
 * challenges.
 * TODO: hyperlinked stuff in the invitation
 * TODO: "not right now button" to workaround the "Ignore" button lowering our allocations
 */
public class FBRequestPanel extends ServerFBMLPanel
{
    /**
     * Creates a panel to contain a generic invite, automatically populating it once the
     * appropriate invite information is returned.
     */
    public static Widget createInvite ()
    {
        // TOOD: some loading message
        final FlowPanel div = new FlowPanel();
        _fbsvc.getInviteInfo(CShell.getAppId(), null, new InfoCallback<InviteInfo>() {
            public void onSuccess (InviteInfo result) {
                // inviteGeneric = {0} has invited you to {1}...
                String invite = _msgs.inviteGeneric(result.username, result.appName);
                String tip = _msgs.inviteGenericTip();
                String accept = _msgs.inviteChallengeAccept(result.appName);

                // accept goes to the main canvas with tracking info
                String[] acceptArgs = result.trackingArgs();

                // submit uploads the tracking info along with facebook's invite data
                String[] submitArgs = result.trackingArgs();

                div.add(new FBRequestPanel(result.excludeIds, invite, tip, accept, acceptArgs,
                    submitArgs, result.appName, result.canvasName));
            }
        });
        return div;
    }

    /**
     * Creates the challenge request panel for the given Facebook game.
     * TODO: high scores?
     */
    public static Widget createChallenge (FacebookGame game, InviteInfo info)
    {
        // {0} just played {1} on {2} and challenges you to beat {3} high score!
        String invite = _msgs.inviteChallenge(
            info.username, info.gameName, info.appName,
            FacebookUtil.getPossessivePronoun(info.gender, false));
        String tip = _msgs.inviteChallengeTip();
        String accept = _msgs.inviteChallengeAccept(info.gameName);

        // on accept, go to the game (via the invite servlet... this will end up viewing the game,
        // but okay)
        String[] acceptArgs = ArrayUtil.concatenate(
            game.getCanvasArgs(), info.trackingArgs(), ArrayUtil.STRING_TYPE);

        // on submit, go to the game but in challenge mode so the feed popup is shown
        String[] submitArgs = ArrayUtil.concatenate(
            acceptArgs, ArgNames.fbChallengeArgs(), ArrayUtil.STRING_TYPE);

        return new FBRequestPanel(info.excludeIds, invite, tip, accept, acceptArgs, submitArgs,
            info.gameName, info.canvasName);
    }

    /**
     * Creates a new invite request form that excludes the given friend ids and has the given
     * translation strings.
     * @param text the invitation copy, such as "Come Play With Me"
     * @param tip mini-instructions at the top of the form, such "Select some friends to play"
     * @param accept the copy on the accept button of the invitation
     * @param acceptArgs array of name/value pairs to add to the accept button url; the accept url
     *        is the one that recipients of the request will go to when they accept the request
     * @param submitArgs array of name/value pairs to add to the submission url; the submit url
     *        is where the current user is redirected to after submitting or canceling the form
     * @param type the type of invitation shown in the form's "send" button
     */
    protected FBRequestPanel (List<Long> excludeIds, String text, String tip, String accept,
        String[] acceptArgs, String[] submitArgs, String type, String canvasName)
    {
        StringBuilder exclude = new StringBuilder();
        if (excludeIds != null) {
            for (Long id : excludeIds) {
                exclude.append(id).append(",");
            }
        }
        String url = SharedNaviUtil.buildRequest(SharedNaviUtil.buildRequest(
            FacebookUtil.getCanvasUrl(canvasName), acceptArgs),
            CookieNames.AFFILIATE, "" + CShell.getMemberId());
        FBMLPanel form = new FBMLPanel("request-form",
            "action", SharedNaviUtil.buildRequest(SharedNaviUtil.buildRequest(
                DeploymentConfig.serverURL + "fbinvite/ndone", submitArgs),
                FBParam.APP_ID.name, String.valueOf(CShell.getAppId())),
            "method", "POST",
            "invite", "true",
            // Facebook ignores escapes in here, sanitize instead
            "type", StringUtil.sanitizeAttribute(type),
            "content", makeContent(text, accept, url));
        form.add(new FBMLPanel("multi-friend-selector",
            "showborder", String.valueOf(false),
            "rows", String.valueOf(6),
            "actiontext", tip,
            "bypass", "cancel",
            "exclude_ids", exclude.toString()));
        add(form);
    }

    protected static String makeContent (String text, String accept, String url)
    {
        // weird... the req-choice tag goes inside the content attribute
        FBMLPanel reqChoice = new FBMLPanel("req-choice", "url", url, "label", accept);
        return StringUtil.escapeAttribute(text) +
            MsoyUI.createFlowPanel(null, reqChoice).getElement().getInnerHTML();
    }

    protected static final FacebookMessages _msgs = GWT.create(FacebookMessages.class);
    protected static final FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
}
