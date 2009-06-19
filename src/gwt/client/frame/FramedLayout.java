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

        int avail = Window.getClientHeight() - getReservedHeight();
        if (bar != null) {
            avail -= FRAMED_NAVI_HEIGHT;
            bar.makeFramed();
            _bar.setWidget(bar);
        }
        // if we have a client, adjust its height...
        if (_client.getWidget() != null) {
            _client.setHeight("300px");
            FlashClients.setClientFullHeight(true);
            avail -= 300;
        }
        content.setHeight(avail + "px");

        _content.setWidget(content);
    }

    @Override // from Layout
    public void closeContent (boolean restoreClient)
    {
        // no content? nothing to do
        if (_content.getWidget() == null) {
            return;
        }

        _bar.setWidget(null);
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
        RootPanel.get(PAGE).add(_client = new SimplePanel());
        RootPanel.get(PAGE).add(_bar = new SimplePanel());
        RootPanel.get(PAGE).add(_content = new SimplePanel());
    }

    /**
     * Gives the number of vertical pixels to save for facebook, oh I mean subclass, page
     * additions.
     */
    protected int getReservedHeight ()
    {
        return 0;
    }

    protected SimplePanel _client, _bar, _content;

    protected static final int FRAMED_NAVI_HEIGHT = 28;
}
