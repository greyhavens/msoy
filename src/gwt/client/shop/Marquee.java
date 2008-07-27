//
// $Id$

package client.shop;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

import client.ui.MsoyUI;

/**
 * Displays a marquee with a fancy background.
 */
public class Marquee extends HorizontalPanel
{
    public Marquee (String icon, String text)
    {
        add(new Image("/images/shop/marquee_left.png"));
        if (icon != null) {
            FlowPanel contents = new FlowPanel();
            contents.setStyleName("marquee");
            contents.add(MsoyUI.createInlineImage(icon));
            contents.add(new Label(text));
            add(contents);
        } else {
            add(MsoyUI.createLabel(text, "marquee"));
        }
        add(new Image("/images/shop/marquee_right.png"));
    }
}
