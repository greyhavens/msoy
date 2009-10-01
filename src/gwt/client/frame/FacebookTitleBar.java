//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.AbsoluteCSSPanel;

import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;
import com.threerings.msoy.web.gwt.WebUserService.AppResult;

import client.facebookbase.FacebookUtil;
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
        _contents = new AbsoluteCSSPanel("fbpageTitle");
        _contents.add(MsoyUI.createFlowPanel("Logo"));
        _contents.add(button("Games", Pages.GAMES));
        _contents.add(button("Invite", Pages.FACEBOOK, "invite"));
        _contents.add(MsoyUI.createImageButton("Fan", new VisitAppProfile()));
        _contents.add(button("Trophies", Pages.GAMES, "t", CShell.getMemberId()));
        _contents.add(new FacebookStatusPanel());
        if (gameName == null) {
            _contents.getElement().setAttribute("mode", "normal");
        } else {
            String challenge = "Click here to challenge a friend to beat you in " + gameName;
            addContextLink(challenge, Pages.FACEBOOK,
                Args.compose(ArgNames.FB_GAME_CHALLENGE, gameId), -1);
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
        // -1 is a special signal that this is a challenge link. other positions are not supported
        if (position != -1) {
            return;
        }

        boolean challenge = !label.isEmpty();

        // set the style attribute for the challenge mode
        _contents.getElement().setAttribute("mode", challenge ? "challenge" : "normal");

        // workaround for IE: attribute selectors do not appear to update dynamically
        // ... so just remove and readd a style attribute here
        DOM.setStyleAttribute(_contents.getElement(), "left", "0px");
        DOM.setStyleAttribute(_contents.getElement(), "left", "");

        // remove the old challenge if any
        if (_challenge != null) {
            _contents.remove(_challenge);
            _challenge = null;
        }

        // add the new one
        if (challenge) {
            _contents.add(_challenge = MsoyUI.createFlowPanel("Challenge",
                Link.create(label, page, args)));
        }
    }

    @Override // from TitleBar
    public void setCloseVisible (boolean visible)
    {
        // not supported
    }

    @Override // from TitleBar
    public boolean isExtended ()
    {
        return _contents.getElement().getAttribute("mode").equals("challenge");
    }

    protected Widget button (String style, Pages page, Object...args)
    {
        return MsoyUI.createImageButton(style, Link.createHandler(page, args));
    }

    protected static class VisitAppProfile
        implements ClickHandler, AsyncCallback<WebUserService.AppResult>
    {
        @Override public void onFailure (Throwable caught) {
        }

        @Override public void onSuccess (AppResult result) {
            Window.open(FacebookUtil.getProfileUrl(result.facebookAppId), "_top", "");
        }

        @Override public void onClick (ClickEvent event) {
            _websvc.getApp(CShell.getAppId(), this);
        }
    }

    protected AbsoluteCSSPanel _contents;
    protected Widget _challenge;

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final WebUserServiceAsync _websvc = GWT.create(WebUserService.class);
}
