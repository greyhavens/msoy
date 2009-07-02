//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import client.images.frame.FrameImages;
import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.util.FlashClients;

/**
 * The standard GWT layout, for use when we've got the whole browser to ourselves.
 */
public class StandardLayout extends Layout
{
    @Override // from Layout
    public boolean hasContent ()
    {
        return _content != null;
    }

    @Override // from Layout
    public void setContent (TitleBar bar, Widget iframe)
    {
        int contentTop = 0;
        String contentWidth = null, contentHeight = null;

        if (bar == null) {
            // content takes up whole page
            contentWidth = "100%";
            contentHeight = "100%";
            contentTop = 0;

            // the content is just the supplied widget, no extra bits
            _content = iframe;

        } else {
            // squish the client if it's around
            if (_client != null) {
                WorldClient.setMinimized(true);
                _client.setWidth(computeClientWidth());
                RootPanel.get(PAGE).setWidgetPosition(_client, CONTENT_WIDTH, NAVI_HEIGHT);
            }

            // position the content normally
            contentWidth = CONTENT_WIDTH + "px";
            contentHeight = (Window.getClientHeight() - NAVI_HEIGHT) + "px";
            contentTop = NAVI_HEIGHT;

            // add a titlebar to the top of the content
            FlowPanel content = new FlowPanel();
            content.add(bar);
            iframe.setWidth(contentWidth);
            iframe.setHeight((Window.getClientHeight() - HEADER_HEIGHT) + "px");
            content.add(iframe);
            _content = content;
        }

        // show the header if we have a title bar, hide it if not
        _header.setVisible(bar != null);

        // size, add and position the content
        _iframe = iframe;
        _content.setWidth(contentWidth);
        _content.setHeight(contentHeight);
        RootPanel.get(PAGE).add(_content);
        RootPanel.get(PAGE).setWidgetPosition(_content, 0, contentTop);

        // on frameless pages, we don't listen for resize because the iframe is height 100%
        setWindowResizerEnabled(_content != iframe);
    }

    @Override // from Layout
    public void setBottomContent (Widget iframe)
    {
        // not supported
    }

    @Override // from Layout
    public void closeContent (boolean restoreClient)
    {
        // not showing, nothing to do
        if (_content == null) {
            return;
        }

        _content = removeFromPage(_content);
        _iframe = null;
        if (restoreClient && _client != null) {
            RootPanel.get(PAGE).setWidgetPosition(_client, 0, NAVI_HEIGHT);
            _client.setWidth("100%");
            _header.setVisible(true);
        }
    }

    @Override // from Layout
    public WorldClient.PanelProvider getClientProvider ()
    {
        return new WorldClient.PanelProvider() {
            public Panel get () {
                // clear out any existing bits
                _noclient = removeFromPage(_noclient);
                _client = removeFromPage(_client);
                _header.setVisible(true);

                Panel client = makeClientPanel();
                RootPanel.get(PAGE).add(client);
                if (_content == null) {
                    client.setWidth("100%");
                    RootPanel.get(PAGE).setWidgetPosition(client, 0, NAVI_HEIGHT);
                } else {
                    client.setWidth(computeClientWidth());
                    RootPanel.get(PAGE).setWidgetPosition(client, CONTENT_WIDTH, NAVI_HEIGHT);
                }
                _client = client;
                return client;
            }
        };
    }

    @Override // from Layout
    public boolean closeClient ()
    {
        if (_client == null) {
            return false;
        }

        _client = removeFromPage(_client);
        addNoClientIcon(); // TODO
        if (_content != null) {
            _content.setWidth(CONTENT_WIDTH + "px");
            _content.setVisible(true);
        }
        return true;
    }

    @Override // from Layout
    public void addNoClientIcon ()
    {
        if (CShell.isGuest() || _client != null) {
            return; // no quick-home link for guests, none if we have a client open
        }

        FlowPanel bits = MsoyUI.createFlowPanel("Bits");
        bits.add(MsoyUI.createPushButton(_images.noclient().createImage(),
                                         _images.noclient_hover().createImage(),
                                         _images.noclient_hover().createImage(), _onGoHome));
        bits.add(MsoyUI.createActionLabel(_cmsgs.goHome(), _onGoHome));

        _noclient = MsoyUI.createSimplePanel(bits, "noclient");
        _noclient.setWidth(computeClientWidth());
        _noclient.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
        RootPanel.get(PAGE).add(_noclient);
        RootPanel.get(PAGE).setWidgetPosition(_noclient, CONTENT_WIDTH, NAVI_HEIGHT);
    }

    @Override // from Layout
    protected void init (FrameHeader header, ClickHandler onGoHome) {
        super.init(header, onGoHome);
        RootPanel.get().addStyleName("standardPage");
        _header.setVisible(false);
        RootPanel.get(PAGE).add(_header);
    }

    protected Widget removeFromPage (Widget widget)
    {
        if (widget != null) {
            RootPanel.get(PAGE).remove(widget);
        }
        return null;
    }

    protected Panel makeClientPanel ()
    {
        Panel client = (Window.getClientHeight() < (NAVI_HEIGHT + FlashClients.getClientHeight())) ?
            new ScrollPanel() : new SimplePanel();
        client.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
        return client;
    }

    protected String computeClientWidth ()
    {
        return Math.max(Window.getClientWidth() - CONTENT_WIDTH, MIN_CLIENT_WIDTH) + "px";
    }

    protected void setWindowResizerEnabled (boolean enabled)
    {
        if (!enabled && _resizer != null) {
            _resizerRegistration.removeHandler();
            _resizer = null;
            _resizerRegistration = null;

        } else if (enabled && _resizer == null) {
            _resizer = new ResizeHandler() {
                public void onResize (ResizeEvent event) {
                    if (_content != null) {
                        _content.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
                    }
                    if (_iframe != null) {
                        _iframe.setHeight((Window.getClientHeight() - HEADER_HEIGHT) + "px");
                    }
                    Widget right = (_client == null) ? _noclient : _client;
                    if (right != null) {
                        // if we have content, the client is in explicitly sized mode and will need
                        // its width updated manually; if we have no content, it is width 100%
                        if (_content != null) {
                            right.setWidth(computeClientWidth());
                        }
                        right.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
                    }
                }
            };
            _resizerRegistration = Window.addResizeHandler(_resizer);
        }
    }

    protected Widget _iframe,  _content, _client, _noclient;

    /** Handles window resizes. */
    protected ResizeHandler _resizer;
    protected HandlerRegistration _resizerRegistration;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final FrameImages _images = (FrameImages)GWT.create(FrameImages.class);

    protected static final int MIN_CLIENT_WIDTH = 300;
}
