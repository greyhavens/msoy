//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.AbsoluteCSSPanel;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays a page title and subnavigation at the top of the page content area for the facebook
 * portal.
 */
public class FacebookTitleBar extends TitleBar
{
    public FacebookTitleBar ()
    {
        this(null, 0);
    }

    public FacebookTitleBar (String gameName, int gameId)
    {
        // temporarily disabled for production, need to implement #facebook-challenge first
        boolean challenge = gameName != null && DeploymentConfig.devDeployment;

        _contents = new AbsoluteCSSPanel("fbpageTitle");
        _contents.getElement().setAttribute("mode", challenge ? "challenge" : "normal");
        _contents.add(MsoyUI.createFlowPanel("Logo"));
        _contents.add(button("Games", Pages.GAMES));
        _contents.add(button("Invite", Pages.FACEBOOK, "invite"));
        _contents.add(Link.createTop("Fan", DeploymentConfig.facebookApplicationUrl));
        _contents.add(button("Trophies", Pages.GAMES, "t", CShell.getMemberId()));
        if (challenge) {
            _contents.add(MsoyUI.createFlowPanel("Challenge", Link.create(
                "Click here to challenge a friend to beat you in " + gameName, Pages.FACEBOOK,
                "challenge", gameId)));
        }
    }

    @Override // from TitleBar
    public Widget exposeWidget ()
    {
        return _contents;
    }

    @Override // from TitleBar
    public void setTitle (String title)
    {
        // not supported? weird
    }

    @Override // from TitleBar
    public void resetNav ()
    {
        // not supported - fb users are always logged in
    }

    @Override // from TitleBar
    public void addContextLink (String label, Pages page, Args args, int position)
    {
        // not supported
    }

    @Override // from TitleBar
    public void setCloseVisible (boolean visible)
    {
        // not supported
    }

    protected Widget button (String style, Pages page, Object...args)
    {
        return MsoyUI.createImageButton(style, Link.createHandler(page, args));
    }

    protected AbsoluteCSSPanel _contents;

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
