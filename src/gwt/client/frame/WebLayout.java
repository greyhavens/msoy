//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.gwt.SessionData;

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
    /** The height of our frame navigation header. */
    public static final int NAVI_HEIGHT = 50 /* header */;

    /** The height of our frame navigation header and page title bar. */
    public static final int HEADER_HEIGHT = NAVI_HEIGHT + 24 /* title bar */;

    /** The maximum width of our content UI, the remainder is used by the world client. */
    public static final int CONTENT_WIDTH = 700;

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
    public Panel prepareClientPanel ()
    {
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

    @Override // from Layout
    public boolean closeClient ()
    {
        if (_client == null) {
            return false;
        }

        _client = removeFromPage(_client);
        positionElements();
        return true;
    }

    @Override // from Layout
    public void addNoClientIcon (SessionData data)
    {
        _topThemes = data.topThemes;
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
        // don't show the whirled map if we have a client open
        if (_client != null) {
            return;
        }

        _noclient = MsoyUI.createSimplePanel(null, "noclient");
        // escape all the quotes in the JSON!
        FlashClients.embedWhirledMap(_noclient, "map=" + _topThemes.replace("\"", "&quot;") +
            "&guest=" + CShell.isGuest());
        RootPanel.get(PAGE).add(_noclient);
    }

    protected <T extends Widget> T removeFromPage (T widget)
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

    protected Widget _iframe,  _content, _client;
    protected Panel _noclient;
    protected String _topThemes;

    /** Handles window resizes. */
    protected ResizeHandler _resizer;
    protected HandlerRegistration _resizerRegistration;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final FrameImages _images = (FrameImages)GWT.create(FrameImages.class);
}
