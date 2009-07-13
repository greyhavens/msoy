//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.AbsoluteCSSPanel;
import com.threerings.gwt.ui.SmartTable;

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
    public FacebookTitleBar (boolean inGame)
    {
        _contents = new AbsoluteCSSPanel("fbpageTitle");
        _contents.add(MsoyUI.createFlowPanel("Logo"));
        _contents.add(button("Games", Pages.GAMES));
        _contents.add(button("Invite", Pages.FACEBOOK, "invite"));
        _contents.add(Link.createTop("Fan", DeploymentConfig.facebookApplicationUrl));
        _contents.add(button("Trophies", Pages.GAMES, CShell.getMemberId()));
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
