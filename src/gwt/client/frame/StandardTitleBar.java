//
// $Id$

package client.frame;

import com.google.gwt.user.client.History;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.Tabs;

import client.shell.DynamicLookup;
import client.shell.Page;
import client.ui.MsoyUI;

/**
 * Displays a page title and subnavigation at the top of the page content area.
 */
public class StandardTitleBar extends TitleBar
{
    /**
     * Creates a title bar for the specified page.
     */
    public static StandardTitleBar create (Tabs tab, ClickHandler onClose, boolean showBackButton)
    {
        return new StandardTitleBar(tab, false, showBackButton, Page.getDefaultTitle(tab),
            new SubNaviPanel(tab), onClose);
    }

    /**
     * Creates a title bar for a game page. This should only be used by layouts that show the
     * title bar while a game is being played: {@link Layout#alwaysShowsTitleBar()}.
     */
    public static StandardTitleBar createGame (ClickHandler onClose, boolean showBackButton)
    {
        String title = _dmsgs.xlate("gameTitle");
        return new StandardTitleBar(null, true, showBackButton, title, new SubNaviPanel(true), onClose);
    }

    /**
     * Creates a title bar for a world page. This should only be used by layouts that show the
     * title bar while the player is in the world: {@link Layout#alwaysShowsTitleBar()}.
     */
    public static StandardTitleBar createWorld (ClickHandler onClose, boolean showBackButton)
    {
        String title = _dmsgs.xlate("sceneTitle");
        return new StandardTitleBar(null, false, showBackButton, title, new SubNaviPanel(true), onClose);
    }

    public StandardTitleBar (Tabs tab, boolean game, boolean showBackButton, String title,
        SubNaviPanel subnavi, ClickHandler onClose)
    {
        _contents = new SmartTable("pageTitle", 0, 0);

        _contents.setWidget(0, 0, createImage(tab), 3, null);

        _tab = tab;
        _game = game;
        _titleLabel = new Label(title);
        _titleLabel.setStyleName("Title");

        HorizontalPanel panel = new HorizontalPanel();
        panel.add(WidgetUtil.makeShim(10, 1));
        if (showBackButton) {
            Widget back = MsoyUI.createImageButton("backButton", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    History.back();
                }
            });
            panel.add(back);
            panel.add(WidgetUtil.makeShim(12, 1));
            panel.setCellVerticalAlignment(back, HorizontalPanel.ALIGN_MIDDLE);
        }
        panel.add(_titleLabel);

        _contents.setWidget(1, 0, panel);
        _contents.setWidget(1, 2, _subnavi = subnavi, 1, "SubNavi");
        _contents.getFlexCellFormatter().setVerticalAlignment(1, 1, HasAlignment.ALIGN_BOTTOM);

        _closeBox = MsoyUI.createActionImage("/images/ui/close.png", onClose);
        _closeBox.addStyleName("Close");
        _closeShim = MsoyUI.createHTML("&nbsp;", "Close");
        setCloseVisible(false);
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

    /**
     * Hacky method to adjust our UI for framed layout mode.
     */
    public void hideTabs ()
    {
        // add a style that overrides some bits
        _contents.addStyleName("framedTitle");
        // clear out the shim image
        _contents.setText(0, 0, "");
    }

    @Override // from TitleBar
    public void resetNav ()
    {
        boolean closeWasVisible = _closeBox.isAttached();
        _subnavi.reset(_tab, _game);
        setCloseVisible(closeWasVisible);
    }

    @Override // from TitleBar
    public void addContextLink (String label, Pages page, Args args, int position)
    {
        _subnavi.addContextLink(label, page, args, position);
    }

    @Override // from TitleBar
    public void setCloseVisible (boolean visible)
    {
        _subnavi.remove(_closeBox);
        _subnavi.remove(_closeShim);
        if (visible) {
            _subnavi.add(_closeBox);
        } else {
            _subnavi.add(_closeShim);
        }
    }

    protected Image createImage (Tabs tab)
    {
        String id = (tab == null) ? "solid" : tab.toString().toLowerCase();
        return new Image("/images/header/" + id + "_cap.png");
    }

    SmartTable _contents;
    protected Tabs _tab;
    protected boolean _game;
    protected Label _titleLabel;
    protected SubNaviPanel _subnavi;
    protected Widget _closeBox, _closeShim;

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
