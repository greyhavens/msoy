//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.Tabs;

import client.shell.DynamicLookup;
import client.shell.Page;

/**
 * Displays a page title and subnavigation at the top of the page content area for the facebook
 * portal.
 */
public class FacebookTitleBar extends TitleBar
{
    public FacebookTitleBar (boolean inGame)
    {
        _contents = new SmartTable("fbpageTitle", 0, 0);
        _subnavi = inGame ? new SubNaviPanel(true) : new SubNaviPanel(Tabs.GAMES);
        _titleLabel = new Label(Page.getDefaultTitle(Tabs.GAMES));
        _titleLabel.setStyleName("Title");
        _contents.setWidget(0, 0, _titleLabel);
        _contents.setWidget(0, 1, _subnavi, 1, "SubNavi");
    }

    @Override // from TitleBar
    public Widget exposeWidget ()
    {
        return _contents;
    }

    @Override // from TitleBar
    public void setTitle (String title)
    {
        if (title != null) {
            _titleLabel.setText(title);
        }
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

    protected SmartTable _contents;
    protected Label _titleLabel;
    protected SubNaviPanel _subnavi; 

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
