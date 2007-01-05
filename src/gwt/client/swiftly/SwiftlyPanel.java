//
// $Id$

package client.swiftly;

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

        add(new Label("Sunrise, sunset, swiftly flow the days"));

        Widget display = WidgetUtil.createApplet(
            "swiftly", "/clients/swiftly-client.jar",
            "com.threerings.msoy.swiftly.client.SwiftlyApplet", "100%", "100%",
            new String[] {});
        display.setHeight("100%");
        add(display);
        setCellHeight(display, "100%");
    }
}
