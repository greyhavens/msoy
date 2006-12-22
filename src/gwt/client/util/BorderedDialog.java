//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.ClickListener;
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

    public BorderedDialog (boolean autoHide)
    {
        this(autoHide, false);
    }

    public BorderedDialog (boolean autoHide, boolean omitCloseBox)
    {
        super(autoHide);

        _main = new VerticalPanel();
        _main.setStyleName("borderedDialog");
        setWidget(_main);

        _main.add(_header = new HorizontalPanel());
        _header.setSpacing(10);
        _header.setStyleName("Title");
        if (!omitCloseBox) {
            _header.add(MsoyUI.createActionLabel("", "CloseBox", new ClickListener() {
                public void onClick (Widget sender) {
                    hide();
                }
            }));
        }
        _main.add(_contents = createContents());
        _main.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT); // right align the footer
        _main.add(_footer = new HorizontalPanel());
        _footer.setSpacing(10);
        _footer.setStyleName("Controls");
    }

    /**
     * Creates the Widget that will contain the contents of this dialog. Do not populate that
     * widget here, just created it.
     */
    protected abstract Widget createContents ();

    protected VerticalPanel _main;
    protected HorizontalPanel _header;
    protected Widget _contents;
    protected HorizontalPanel _footer;
}
