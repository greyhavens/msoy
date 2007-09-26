//
// $Id$

package client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.TabPanel;
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
        this(autoHide, omitCloseBox, true);
    }
    
    public BorderedDialog (boolean autoHide, boolean omitCloseBox, boolean enableDrag)
    {
        super(autoHide);

        _main = new VerticalPanel();
        _main.setStyleName("borderedDialog");
        setWidget(_main);

        FlexTable headerBackground = new FlexTable();
        headerBackground.setStyleName("HeaderBackground");
        headerBackground.setCellSpacing(0);
        headerBackground.setCellPadding(0);
        headerBackground.getCellFormatter().setStyleName(0, 0, "HeaderLeft");
        headerBackground.getCellFormatter().setStyleName(0, 1, "HeaderCenter");
        headerBackground.getCellFormatter().setHorizontalAlignment(0, 1, 
            HorizontalPanel.ALIGN_CENTER);
        headerBackground.getCellFormatter().setStyleName(0, 2, "HeaderRight");

        Label dragLabel = new Label();
        dragLabel.setStyleName("HeaderDrag");
        dragLabel.setWidth("100%");
        if (enableDrag) {
            dragLabel.addMouseListener(createDragListener());
        }

        Grid headerTitle = new Grid(1, 3);
        headerTitle.setStyleName("HeaderTitle");
        headerTitle.setCellSpacing(0);
        headerTitle.setCellPadding(0);
        headerTitle.getCellFormatter().setStyleName(0, 0, "TitleLeft");
        headerTitle.getCellFormatter().setStyleName(0, 1, "TitleCenter");
        headerTitle.getCellFormatter().setStyleName(0, 2, "TitleRight");
        headerTitle.setWidget(0, 1, _header = new HorizontalPanel());
        headerBackground.setWidget(0, 1, headerTitle);

        FlowPanel titleBar = new FlowPanel();
        titleBar.setStyleName("TitleBar");
        titleBar.add(headerBackground);
        titleBar.add(dragLabel);
        
        _main.add(titleBar);
        _header.setSpacing(0);
        _header.setStyleName("Title");
        _header.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        if (!omitCloseBox) {
            _header.add(MsoyUI.createActionLabel("", "CloseBox", new ClickListener() {
                public void onClick (Widget sender) {
                    hide();
                }
            }));
        }
        _main.add(_contents = createContents());
        _contents.setWidth("100%");

        _main.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
        _main.setVerticalAlignment(VerticalPanel.ALIGN_BOTTOM);
        _main.add(_footer = new HorizontalPanel());
        // there's no way to do this through normal channels
        DOM.setAttribute(DOM.getParent(_footer.getElement()), "className", "Footer");
        _footer.setSpacing(10);
        _footer.setStyleName("Controls");
    }

    protected Label createTitleLabel (String text, String style)
    {
        Label label = new Label(text);
        if (style != null) {
            label.setStyleName(style);
        }
        return label;
    }

    /**
     * Creates the Widget that will contain the contents of this dialog. Do not populate that
     * widget here, just created it.
     */
    protected abstract Widget createContents ();

    /** Creates the drag listener. */
    protected MouseListenerAdapter createDragListener () {
        return new MouseListenerAdapter() {
            public void onMouseDown (Widget sender, int x, int y) {
                _dragging = true;
                DOM.setCapture(sender.getElement());
                _dragStartX = x;
                _dragStartY = y;
            }

            public void onMouseMove (Widget sender, int x, int y) {
                if (_dragging) {
                    int absX = x + getAbsoluteLeft();
                    int absY = y + getAbsoluteTop();
                    setPopupPosition(absX - _dragStartX, absY - _dragStartY);
                    updateFrame();
                }
            }

            public void onMouseUp (Widget sender, int x, int y) {
                _dragging = false;
                DOM.releaseCapture(sender.getElement());
            }

            protected boolean _dragging;
            protected int _dragStartX, _dragStartY;
        };
    }

    protected VerticalPanel _main;
    protected HorizontalPanel _header;
    protected Widget _contents;
    protected HorizontalPanel _footer;
}
