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
        Widget display;
        
        add(new Label("Sunrise, sunset, swiftly flow the days"));
        display = WidgetUtil.createApplet(
            "swiftly", "/clients/swiftly-client.jar",
            "com.threerings.msoy.swiftly.client.SwiftlyApplet", 800, 600,
            new String[] {});

        add(display);
    }
}
