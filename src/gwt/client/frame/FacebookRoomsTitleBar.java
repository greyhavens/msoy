//
// $Id$

package client.frame;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.AbsoluteCSSPanel;
import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.Tabs;

import client.ui.MsoyUI;

public class FacebookRoomsTitleBar extends TitleBar
{
    public FacebookRoomsTitleBar (Tabs tab, ClickHandler onClose)
    {
        _tab = tab;
        _onClose = onClose;

        _contents = new AbsoluteCSSPanel("pageTitle");
        _contents.addStyleName("framedTitle");
        _contents.addStyleName("fbRoomsTitle");
        _contents.add(MsoyUI.createFlowPanel("Logo"));
        _contents.add(_subnavi = new SubNaviPanel(tab));
        _contents.add(_titleLabel = MsoyUI.createLabel(LOADING, "Title"));
        _contents.add(new FacebookStatusPanel());

        _subnavi.addStyleName("SubNavi");
    }

    @Override // from TitleBar
    public Widget exposeWidget ()
    {
        return _contents;
    }

    @Override // from TitleBar
    public void setTitle (String title)
    {
        _titleLabel.setText(StringUtil.getOr(title, LOADING));
    }

    @Override // from TitleBar
    public void resetNav ()
    {
        _subnavi.reset(_tab);
    }

    @Override // from TitleBar
    public void addContextLink (String label, Pages page, Args args, int position)
    {
        _subnavi.addContextLink(label, page, args, position);
    }

    @Override // from TitleBar
    public void setCloseVisible (boolean visible)
    {
        // TODO
    }

    @Override // from TitleBar
    public Widget createCloseBox ()
    {
        Widget closeBox = MsoyUI.createActionImage("/images/ui/close.png", _onClose);
        closeBox.addStyleName("Close");
        return MsoyUI.createFlowPanel("fbRoomsCloseBar", closeBox);
    }

    protected Tabs _tab;
    protected AbsoluteCSSPanel _contents;
    protected SubNaviPanel _subnavi;
    protected Label _titleLabel;
    protected ClickHandler _onClose;

    // TODO: temp
    protected static final String LOADING = "Loading...";
}
