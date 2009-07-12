//
// $Id$

package client.frame;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import client.util.FlashClients;

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

        if (bar != null) {
            bar.makeFramed();
            _bar.setWidget(bar);
        }

        // if we have a client, adjust its height...
        if (_client.getWidget() != null) {
            _client.setHeight("300px");
            FlashClients.setClientFullHeight(true);
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
            _client.setHeight(FlashClients.getClientHeight() + "px");
        }
    }

    @Override // from Layout
    public WorldClient.PanelProvider getClientProvider ()
    {
        return new WorldClient.PanelProvider() {
            public Panel get () {
                closeClient();
                _client.setHeight(null);
                FlashClients.setClientFullHeight(false);
                return _client;
            }
        };
    }

    @Override // from Layout
    public boolean closeClient ()
    {
        if (_client == null) {
            return false;
        }
        FlashClients.setClientFullHeight(false);
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
            avail -= FRAMED_NAVI_HEIGHT;
        }
        if (_client.getWidget() != null) {
            avail -= 300;
        }
        return avail;
    }

    protected SimplePanel _client, _bar, _content;

    protected static final int FRAMED_NAVI_HEIGHT = 28;
}
