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
 * The standard three section layout: client, header and content. The header is further divided
 * into logo, status and navigation, each of which are positioned indiviually. For use when we've
 * got the whole browser to ourselves.
 */
public abstract class WebLayout extends Layout
{
    @Override // from Layout
    public boolean hasContent ()
    {
        return _content != null;
    }

    @Override // from Layout
    public void setContent (TitleBar bar, Widget iframe)
    {
        _iframe = iframe;
        if (bar == null) {
            // the content is just the supplied widget, no extra bits
            _content = iframe;

        } else {
            // squish the client if it's around
            if (_client != null) {
                WorldClient.setMinimized(true);
            }

            _content = MsoyUI.createFlowPanel(null, bar.exposeWidget(), _iframe);
        }

        // add the content
        _content.setVisible(false);
        RootPanel.get(PAGE).add(_content);

        // position 
        positionElements();
        _content.setVisible(true);
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
            positionElements();
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

                // add a new client panel
                Panel client = makeClientPanel();
                RootPanel.get(PAGE).add(_client = client);

                // reposition everything
                positionElements();
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
        doAddNoClientIcon(); // TODO
        positionElements();
        return true;
    }

    @Override // from Layout
    public void addNoClientIcon ()
    {
        doAddNoClientIcon();
        positionElements();
    }

    @Override // from Layout
    public void updateTitleBarHeight ()
    {
        // not supported
    }

    @Override // from Layout
    protected void init (FrameHeader header, ClickHandler onGoHome) {
        super.init(header, onGoHome);
        RootPanel.get().addStyleName("standardPage");
        _header.setVisible(false);
        RootPanel.get(PAGE).add(_header.getLogo());
        RootPanel.get(PAGE).add(_header.getStatusPanel());
        RootPanel.get(PAGE).add(_header.getNaviPanel());
    }

    protected void doAddNoClientIcon ()
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
        RootPanel.get(PAGE).add(_noclient);
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
        int minHeight = FlashClients.MIN_WORLD_HEIGHT;
        Panel client = (Window.getClientHeight() < (NAVI_HEIGHT + minHeight)) ?
            new ScrollPanel() : new SimplePanel();
        client.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
        return client;
    }

    protected String computeClientWidth ()
    {
        return Math.max(
            Window.getClientWidth() - CONTENT_WIDTH, FlashClients.MIN_WORLD_WIDTH) + "px";
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
                    positionElements();
                }
            };
            _resizerRegistration = Window.addResizeHandler(_resizer);
        }
    }

    /**
     * Positions all of the page contents, called after anything changes.
     */
    protected abstract void positionElements ();

    protected Widget _iframe,  _content, _client, _noclient;

    /** Handles window resizes. */
    protected ResizeHandler _resizer;
    protected HandlerRegistration _resizerRegistration;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final FrameImages _images = (FrameImages)GWT.create(FrameImages.class);
}
