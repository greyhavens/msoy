package client.frame;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Standard web layout for when we have the whole browser to ourselves. Flash client in the middle,
 * GWT on the right and header components along the top.
 */
public class StandardLayout extends WebLayout
{
    @Override
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

        int clientWidth = Window.getClientWidth();
        clientWidth -= (_content == null) ? 0 : CONTENT_WIDTH;
        clientWidth = Math.max(clientWidth, MIN_CLIENT_WIDTH);

        if (_content != null) {
            _content.setWidth(CONTENT_WIDTH + "px");
            _content.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
            _iframe.setWidth(CONTENT_WIDTH + "px");
            _iframe.setHeight((Window.getClientHeight() - HEADER_HEIGHT) + "px");
            RootPanel.get(PAGE).setWidgetPosition(_content, clientWidth, NAVI_HEIGHT);
        }

        Widget client = _client != null ? _client : _noclient;
        if (client != null) {
            client.setWidth(clientWidth + "px");
            client.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
            RootPanel.get(PAGE).setWidgetPosition(client, 0, NAVI_HEIGHT);
        }

        // take care of header elements
        _header.setVisible(true);
        int logoWidth = 126;
        int naviLeft = Math.max(MIN_CLIENT_WIDTH, Window.getClientWidth() - CONTENT_WIDTH);
        RootPanel.get(PAGE).setWidgetPosition(_header.getLogo(), 0, 0);
        RootPanel.get(PAGE).setWidgetPosition(_header.getNaviPanel(), naviLeft, 0);
        RootPanel.get(PAGE).setWidgetPosition(_header.getStatusPanel(), logoWidth, 0);

        // turn on resizer
        setWindowResizerEnabled(true);
    }
}
