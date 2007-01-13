//
// $Id$

package client.swiftly;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

/**
 * Displays the client interface for a particular game.
 */
public class SwiftlyPanel extends VerticalPanel
{
    public SwiftlyPanel (SwiftlyContext ctx)
    {
        setStyleName("swiftlyPanel");

        // Work around GWT awesomeness.
        final SwiftlyContext localCtx = ctx;

        // TODO: make this return server hostname and port
        ctx.swiftlysvc.getRpcURL(new AsyncCallback() {
            public void onSuccess (Object result) {
                SwiftlyPanel.this.loadApplet(localCtx, (String)result);
            }
            public void onFailure (Throwable cause) {
                localCtx.serverError(cause);
            }
        });
    }

    public void loadApplet (SwiftlyContext ctx, String rpcURL)
    {
        String authtoken = (ctx.creds == null) ? "" : ctx.creds.token;
        add(new Label("Sunrise, sunset, swiftly flow the days"));

        Widget display = WidgetUtil.createApplet(
            "swiftly", "/clients/swiftly-client.jar",
            "com.threerings.msoy.swiftly.client.SwiftlyApplet", "100%", "100%",
            new String[] {  "authtoken", authtoken,
                            "projectId", "1",
                            "server", "localhost", // TODO: unhack!
                            "port", "4010",
                            "rpcURL", rpcURL });
        display.setHeight("100%");
        add(display);
        setCellHeight(display, "94%");
    }
}
