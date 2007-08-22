//
// $Id$

package client.util;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HorizontalPanel;

/**
 * Works around browser's bullshit inability to put fucking spacing between cells in CSS without
 * also putting it around the outer edge of the whole table. Yay!
 */
public class RowPanel extends HorizontalPanel
{
    // @Override // from ComplexPanel
    protected void add (Widget child, Element container)
    {
        super.add(child, container);
        setStyleName(container, "rowPanelCell", true);
    }
}
