//
// $Id$

package client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

/**
 * Displays a widget with a nice, configurable border around it. Widgets that are tiled adjacent
 * to one another need to explicitly match their heights/widths for things to look good.
 * 
 */
public class BorderedWidget extends FlexTable
{
    /** A fully closed and cornered border. */
    public static final BorderState BORDER_CLOSED = new BorderState();
    /** A segmenting bar between two bordered widgets. */
    public static final BorderState BORDER_TILED = new BorderState();
    /** An entirely open border against another bordered widget. */
    public static final BorderState BORDER_OPEN = new BorderState();


    public BorderedWidget ()
    {
        this(BORDER_CLOSED, BORDER_CLOSED, BORDER_CLOSED, BORDER_CLOSED);
    }
    
    public BorderedWidget (BorderState left, BorderState right,
                           BorderState up, BorderState down)
    {
        setStyleName("borderedWidget");

        DOM.setAttribute(getElement(), "cellSpacing", "0");
        DOM.setAttribute(getElement(), "cellPadding", "0");

        FlexCellFormatter formatter = getFlexCellFormatter();

        byte nw = 0, w = 0, sw = 0;
        byte n = 0, s = 0;
        byte ne = 0, e = 0, se = 0;

        /* we construct one of eleven tiles:
         * 
         * Four corners: P10, P9, P6, P5
         *   ****   ****   *--*   *--*
         *   *---   ---*   *---   ---*
         *   *---   ---*   *---   ---*
         *   *--*   *--*   ****   ****
         *   
         * Straight lines P3, P12
         *   ****   *--*
         *   ----   *--*
         *   ----   *--*
         *   ****   *--*
         *   
         * Three-ways P13, P14, P11, P7:
         * 
         *   *--*   *--*   *--*   ****
         *   ---*   *---   ----   ----
         *   ---*   *---   ----   ----
         *   *--*   *--*   ****   *--*
         *   
         * And P15.
         *   *--*
         *   ----
         *   ----
         *   *--*
         *   
         * i.e. every permutation except the trivial P0, P1, P2, P4 and P8.
         */

        if (left == BORDER_CLOSED) {
            nw |= DOWN | RIGHT; w |= UP | DOWN; sw |= UP | RIGHT;
        } else if (left == BORDER_TILED) {
            nw |= LEFT | RIGHT | DOWN; w |= UP | DOWN; sw |= LEFT | RIGHT | UP;
        } else {
            nw |= LEFT; sw |= LEFT;
        }
        if (up == BORDER_CLOSED) {
            nw |= RIGHT | DOWN; n |= LEFT | RIGHT; ne |= LEFT | DOWN;
        } else if (up == BORDER_TILED) {
            nw |= UP | DOWN | RIGHT; n |= LEFT | RIGHT; ne |= UP | DOWN | LEFT;
        } else {
            nw |= UP | DOWN; ne |= UP | DOWN;
        }
        if (right == BORDER_CLOSED) {
            ne |= DOWN | LEFT; e |= UP | DOWN; se |= UP | LEFT;
        } else {
            ne |= LEFT | RIGHT; se |= LEFT | RIGHT;
        }
        if (down == BORDER_CLOSED) {
            sw |= RIGHT | UP; s |= LEFT | RIGHT; se |= LEFT | UP; 
        } else {
            sw |= UP | DOWN; se |= UP | DOWN;
        }

        formatter.setStyleName(0, 0, toCSS(nw));
        formatter.setStyleName(1, 0, toCSS(w));
        formatter.setStyleName(2, 0, toCSS(sw));
        formatter.setStyleName(0, 1, toCSS(n));
        formatter.setStyleName(2, 1, toCSS(s));
        formatter.setStyleName(0, 2, toCSS(ne));
        formatter.setStyleName(1, 2, toCSS(e));
        formatter.setStyleName(2, 2, toCSS(se));
    }

    public void setWidget (Widget contents)
    {
        setWidget(1, 1, contents);
    }

    public void setVerticalAlignment (VerticalAlignmentConstant alignment)
    {
        getCellFormatter().setVerticalAlignment(1, 1, alignment);
        
    }

    protected String toCSS (byte mask)
    {
        String out = "Border";
        if ((mask & LEFT) != 0) out += "L";
        if ((mask & RIGHT) != 0) out += "R";
        if ((mask & UP) != 0) out += "U";
        if ((mask & DOWN) != 0) out += "D";
        return out;
    }

    protected static class BorderState
    {
    }

    protected static byte LEFT = 1;
    protected static byte RIGHT = 2;
    protected static byte UP = 4;
    protected static byte DOWN = 8;
}
