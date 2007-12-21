//
// $Id$

package client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
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
        addStyleName("borderedWidget");
        setCellSpacing(0);
        setCellPadding(0);
        setBorderStates(left, right, up, down);
    }

    public void setBorderStates (BorderState left, BorderState right,
                                 BorderState up, BorderState down)
    {
        _left = left;
        _right = right;
        _up = up;
        _down = down;

        layout();
    }

    public void setLeftBorderState (BorderState state)
    {
        _left = state;
        layout();
    }

    public void setRightBorderState (BorderState state)
    {
        _right = state;
        layout();
    }

    public void setUpperBorderState (BorderState state)
    {
        _up = state;
        layout();
    }

    public void setLowerBorderState (BorderState state)
    {
        _down = state;
        layout();
    }

    protected void layout ()
    {
        CellFormatter formatter = getCellFormatter();

        int x = 0, y;
        if (_left == BORDER_CLOSED) {
            y = 0;
            if (_up == BORDER_CLOSED) {
                formatter.addStyleName(y ++, x, "northWestOut");
            }
            if (_up != BORDER_OPEN) {
                formatter.addStyleName(y ++, x, "westOut");
            }
            formatter.addStyleName(y ++, x, "westOut");
            if (_down != BORDER_OPEN) {
                formatter.addStyleName(y ++, x, "westOut");
            }
            if (_down == BORDER_CLOSED) {
                formatter.addStyleName(y ++, x, "southWestOut");
            }
            x += 1;
        }
        if (_left != BORDER_OPEN) {
            y = 0;
            if (_up == BORDER_CLOSED) {
                formatter.addStyleName(y++, x, "northOut");
            }
            if (_up != BORDER_OPEN) {
                formatter.addStyleName(y++, x, "northWestIn");
            }
            formatter.addStyleName(y++, x, "westIn");
            if (_down != BORDER_OPEN) {
                formatter.addStyleName(y++, x, "southWestIn");
            }
            if (_down == BORDER_CLOSED) {
                formatter.addStyleName(y++, x, "southOut");
            }
            x += 1;
        }
        y = 0;
        if (_up == BORDER_CLOSED) {
            formatter.addStyleName(y++, x, "northOut");
        }
        if (_up != BORDER_OPEN) {
            formatter.addStyleName(y++, x, "northIn");
        }
        _tileX = x;
        _tileY = y;
        y += 1;
        if (_down != BORDER_OPEN) {
            formatter.addStyleName(y++, x, "southIn");
        }
        if (_down == BORDER_CLOSED) {
            formatter.addStyleName(y++, x, "southOut");
        }
        x += 1;
        if (_right != BORDER_OPEN) {
            y = 0;
            if (_up == BORDER_CLOSED) {
                formatter.addStyleName(y++, x, "northOut");
            }
            if (_up != BORDER_OPEN) {
                formatter.addStyleName(y++, x, "northEastIn");
            }
            formatter.addStyleName(y++, x, "eastIn");
            if (_down != BORDER_OPEN) {
                formatter.addStyleName(y++, x, "southEastIn");
            }
            if (_down == BORDER_CLOSED) {
                formatter.addStyleName(y++, x, "southOut");
            }
            x += 1;
        }
        if (_right == BORDER_CLOSED) {
            y = 0;
            if (_up == BORDER_CLOSED) {
                formatter.addStyleName(y ++, x, "northEastOut");
            }
            if (_up != BORDER_OPEN) {
                formatter.addStyleName(y ++, x, "eastOut");
            }
            formatter.addStyleName(y ++, x, "eastOut");
            if (_down != BORDER_OPEN) {
                formatter.addStyleName(y ++, x, "eastOut");
            }
            if (_down == BORDER_CLOSED) {
                formatter.addStyleName(y ++, x, "southEastOut");
            }
            x += 1;
        }
    }

    public void setWidget (Widget contents)
    {
        setWidget(_tileY, _tileX, contents);
    }

    public void setVerticalAlignment (VerticalAlignmentConstant alignment)
    {
        getCellFormatter().setVerticalAlignment(_tileY, _tileX, alignment);
    }

    public void setHorizontalAlignment (HorizontalAlignmentConstant alignment)
    {
        getCellFormatter().setHorizontalAlignment(_tileY, _tileX, alignment);
    }

    protected static class BorderState
    {
    }

    protected BorderState _left, _right, _up, _down;

    protected int _tileX, _tileY;

    protected static byte LEFT = 1;
    protected static byte RIGHT = 2;
    protected static byte UP = 4;
    protected static byte DOWN = 8;
}
