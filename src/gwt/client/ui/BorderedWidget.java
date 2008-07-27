//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays a widget with a border around it.
 */
public class BorderedWidget extends SimplePanel
{
    public BorderedWidget ()
    {
        addStyleName("borderedWidget");
        super.setWidget(_inner = new SimplePanel());
        _inner.setStyleName("Inner");
    }

    public void setWidget (Widget contents)
    {
        _inner.setWidget(contents);
    }

    protected SimplePanel _inner;
}
