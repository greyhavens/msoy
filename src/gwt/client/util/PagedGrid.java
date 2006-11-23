//
// $Id$

package client.util;

import java.util.ArrayList;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays a grid of UI elements in pages.
 */
public abstract class PagedGrid extends Grid
{
    /**
     * Creates a grid of the specified dimensions (a pox on Google for asking
     * for height, width instead of width, height).
     */
    public PagedGrid (int rows, int columns)
    {
        // leave an extra row for the next/prev buttons
        super(rows+1, columns);

        // these will be used for navigation
        _next = new Button("Next");
        _next.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                displayPage(_page+1, false);
            }
        });
        _prev = new Button("Prev");
        _prev.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                displayPage(_page-1, false);
            }
        });
    }

    /**
     * Displays the specified page. Does nothing if we are already displaying
     * that page unless forceRefresh is true.
     */
    public void displayPage (int page, boolean forceRefresh)
    {
        if (_page == page && !forceRefresh) {
            return; // NOOP!
        }
        _page = Math.max(page, 0);
        clear();

        if (_items == null || _items.size() == 0) {
            setText(0, 0, getEmptyMessage());
            return;
        }

        int rows = numRows - 1; // last row is reserved
        int count = numColumns * rows, start = numColumns * rows * page;
        int limit = Math.min(count, _items.size()-start), row = -1;
        for (int ii = 0; ii < limit; ii++) {
            setWidget(row = (ii / numColumns), ii % numColumns, createWidget(_items.get(ii+start)));
        }

        setWidget(row+1, 0, _prev);
        _prev.setEnabled(start > 0);
        setWidget(row+1, numColumns-1, _next);
        getCellFormatter().setHorizontalAlignment(
            row+1, numColumns-1, HasHorizontalAlignment.ALIGN_RIGHT);
        _next.setEnabled(start+limit < _items.size());
    }

    /**
     * Returns true if this grid has been configured with items or null if
     * {@link #setItems} has never been called. Note: it may have been
     * configured with zero items in which case it will still return true.
     */
    public boolean hasItems ()
    {
        return (_items != null);
    }

    /**
     * Configures this grid with its underlying items.
     */
    public void setItems (ArrayList items)
    {
        _items = items;
        displayPage(_page, true);
    }

    protected abstract Widget createWidget (Object item);

    protected abstract String getEmptyMessage ();

    protected Button _next, _prev;

    protected ArrayList _items;
    protected int _page;
}
