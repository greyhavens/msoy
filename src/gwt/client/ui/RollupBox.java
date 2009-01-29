//
// $Id$

package client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Standard box that contains a header; when the header is clicked, the contents are opened up
 * within the box below the header.  The header contains an arrow and a title.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class RollupBox extends VerticalPanel
{
    /**
     * @param header Text to display in the header.
     * @param styleName Name of the style for this rollup box, or null if not specially styled.
     * @param contents Widget to display in the contents when opened up.
     */
    public RollupBox (String header, String styleName, Widget contents)
    {
        _contents = contents;
        setStylePrimaryName("rollupBox");
        if (styleName != null) {
            addStyleName(styleName);
        }

        // Rollup box consists of two sections: header at the top and contents at the bottom.
        HorizontalPanel headerPanel = new HorizontalPanel() {
            @Override public void onBrowserEvent (Event event) {
                if (DOM.eventGetType(event) == Event.ONCLICK) {
                    setOpen(!_open);
                }
            }
        };
        headerPanel.setStylePrimaryName("rollupBox-header");
        headerPanel.sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS);
        headerPanel.setWidth("100%");

        _arrow = new SimplePanel();
        _arrow.addStyleName("rollupBox-arrow");
        headerPanel.add(_arrow);

        _headerLabel = new Label(header);
        _headerLabel.addStyleName("rollupBox-title");
        headerPanel.add(_headerLabel);
        headerPanel.setCellWidth(_headerLabel, "100%");
        add(headerPanel);

        _open = true;
        setOpen(false);
    }

    /**
     * Opens or closes the rollup box.
     * @param open If true, ensures the box is opened.  Otherwise, ensures it is closed.
     */
    public void setOpen (boolean open)
    {
        if (open == _open)
            return;

        if (open) {
            _headerLabel.addStyleName("rollupBox-title-open");
            _arrow.addStyleName("rollupBox-arrow-open");
            add(_contents);
        } else {
            _headerLabel.removeStyleName("rollupBox-title-open");
            _arrow.removeStyleName("rollupBox-arrow-open");
            remove(_contents);
        }

        _open = open;
    }

    protected final Widget _contents;
    protected final Label _headerLabel;
    protected final Widget _arrow;
    protected boolean _open;
}
