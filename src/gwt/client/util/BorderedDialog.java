//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays a bordered popup with an area for a title, an area for contents and an area for
 * controls.
 */
public abstract class BorderedDialog extends BorderedPopup
{
    public BorderedDialog ()
    {
        this(false);
    }

    public BorderedDialog (boolean autohide)
    {
        super(autohide);

        _main = new VerticalPanel();
        _main.setStyleName("borderedDialog");
        setWidget(_main);

        _main.add(_header = new HorizontalPanel());
        _header.setStyleName("Title");
        _main.add(_contents = createContents());
        _main.add(_footer = new HorizontalPanel());
        _footer.setStyleName("Controls");
    }

    protected abstract Widget createContents ();

    protected VerticalPanel _main;
    protected HorizontalPanel _header;
    protected Widget _contents;
    protected HorizontalPanel _footer;
}
