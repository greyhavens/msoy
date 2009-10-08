//
// $Id$

package client.frame;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * The old standard web layout, GWT on the left, flash client on the right, header components
 * along the top.
 */
public class OldStandardLayout extends WebLayout
{
    protected void positionElements ()
    {
        if (_content != null && _content == _iframe) {
            // content takes up whole page (client should not be visible)
            _content.setWidth("100%");
            _content.setHeight("100%");
            RootPanel.get(PAGE).setWidgetPosition(_content, 0, 0);

            _header.setVisible(false);
            setWindowResizerEnabled(false);
            return;
        }

        if (_content != null) {
            _content.setWidth(CONTENT_WIDTH + "px");
            _content.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
            RootPanel.get(PAGE).setWidgetPosition(_content, 0, NAVI_HEIGHT);

            _iframe.setWidth(CONTENT_WIDTH + "px");
            _iframe.setHeight((Window.getClientHeight() - HEADER_HEIGHT) + "px");
        }

        Widget client = _client != null ? _client : _noclient;
        if (client != null) {
            if (_content != null) {
                int width = Math.max(Window.getClientWidth() - CONTENT_WIDTH, MIN_CLIENT_WIDTH);
                client.setWidth(width + "px");
                RootPanel.get(PAGE).setWidgetPosition(client, CONTENT_WIDTH, NAVI_HEIGHT);
            } else {
                client.setWidth("100%");
                RootPanel.get(PAGE).setWidgetPosition(client, 0, NAVI_HEIGHT);
            }
            client.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
        }

        // take care of header elements
        _header.setVisible(true);
        int logoWidth = 126;
        RootPanel.get(PAGE).setWidgetPosition(_header.getLogo(), 0, 0);
        RootPanel.get(PAGE).setWidgetPosition(_header.getNaviPanel(), logoWidth, 0);
        RootPanel.get(PAGE).setWidgetPosition(_header.getStatusPanel(), CONTENT_WIDTH, 0);

        // turn on resizer
        setWindowResizerEnabled(true);
    }
}
