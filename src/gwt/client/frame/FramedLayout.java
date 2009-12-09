//
// $Id$

package client.frame;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A slimmed down vertical layout for use when Whirled is embedded in an iframe on another
 * site. *cough* *cough* Facebook *cough*.
 */
public class FramedLayout extends Layout
{
    @Override // from Layout
    public boolean hasContent ()
    {
        return _content.getWidget() != null;
    }

    @Override // from Layout
    public void setContent (TitleBar bar, Widget content)
    {
        closeContent(false);
        content.setWidth("100%");

        Widget barWidget = bar != null ? bar.exposeWidget() : null;
        _bar.setWidget(barWidget);

        // if we have a client, adjust its height...
        if (_client.getWidget() != null) {
            _client.setHeight(CLIENT_MINIMIZED_HEIGHT + "px");
        }

        _content.setWidget(content);
        updateMainContentHeight();
    }

    @Override // from Layout
    public void setBottomContent (Widget content)
    {
        // not supported
    }

    @Override // from Layout
    public void closeContent (boolean restoreClient)
    {
        // no content? nothing to do
        if (_content.getWidget() == null) {
            return;
        }

        if (!alwaysShowsTitleBar()) {
            _bar.setWidget(null);
        }
        _content.setWidget(null);
        if (_client.getWidget() != null) {
            _client.setHeight(CLIENT_HEIGHT + "px");
        }
    }

    @Override // from Layout
    public Panel prepareClientPanel ()
    {
        closeClient();
        _client.setHeight((_content.getWidget() == null ?  CLIENT_HEIGHT :
            CLIENT_MINIMIZED_HEIGHT) + "px");
        updateMainContentHeight();
        return _client;
    }

    @Override // from Layout
    public boolean closeClient ()
    {
        if (_client == null) {
            return false;
        }
        _client.setHeight(null);
        _client.setWidget(null);
        return true;
    }

    @Override // from Layout
    public void addNoClientIcon ()
    {
        // not supported
    }

    @Override // from Layout
    protected void init (FrameHeader header, ClickHandler onGoHome)
    {
        super.init(header, onGoHome);
        RootPanel.get().addStyleName("framedPage");
        _client = new SimplePanel();
        _bar = new SimplePanel();
        _content = new SimplePanel();
        addPanels(RootPanel.get(PAGE));
    }

    @Override // from Layout
    public boolean usesFramedTitleBar ()
    {
        return true;
    }

    @Override // from Layout
    public void updateTitleBarHeight ()
    {
        // not supported
    }

    /**
     * Adds our previously created panel members to the root panel. Subclasses can change the order
     * or whatever.
     */
    protected void addPanels (RootPanel root)
    {
        root.add(_client);
        root.add(_bar);
        root.add(_content);
    }

    protected void updateMainContentHeight ()
    {
        if (_content.getWidget() != null) {
            _content.getWidget().setHeight(calcMainContentHeight() + "px");
        }
    }

    protected int calcMainContentHeight ()
    {
        int avail = Window.getClientHeight();
        if (_bar.getWidget() != null) {
            avail -= getTitleBarHeight();
        }
        if (_client.getWidget() != null) {
            avail -= CLIENT_MINIMIZED_HEIGHT;
        }
        return avail;
    }

    protected int getTitleBarHeight ()
    {
        return 28;
    }

    protected SimplePanel _client, _bar, _content;
    protected static final int CLIENT_HEIGHT = 545;
    protected static final int CLIENT_MINIMIZED_HEIGHT = 300;
}
