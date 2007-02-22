//
// $Id$

package client.swiftly;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.data.SwiftlyConfig;

/**
 * Displays the client interface for a particular swiftly project.
 */
public class SwiftlyPanel extends VerticalPanel
{
    public SwiftlyPanel (SwiftlyConfig config, int projectId)
    {
        super();
        setStyleName("swiftlyPanel");

        String authtoken = (CSwiftly.creds == null) ? "" : CSwiftly.creds.token;
        add(new Label("Sunrise, sunset, swiftly flow the days"));

        Widget display = WidgetUtil.createApplet(
            "swiftly", "/clients/swiftly-client.jar",
            "com.threerings.msoy.swiftly.client.SwiftlyApplet", "800", "600",
            new String[] {  "authtoken", authtoken,
                            "projectId", String.valueOf(projectId),
                            "server", config.server,
                            "port", String.valueOf(config.port) });
        // TODO: possible hacks to get the applet to stay the size of the browser window
        // 100% 100% should work but isn't for the createApplet width/height
        // display.setHeight("100%");
        add(display);
        // setCellHeight(display, "94%");
    }
}
