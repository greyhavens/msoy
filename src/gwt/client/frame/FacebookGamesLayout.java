//
// $Id$

package client.frame;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A frame layout that adds an extra iframe below the main gwt page showing facebook information
 * such as friend scores etc.
 */
public class FacebookGamesLayout extends FacebookLayout
{
    @Override
    protected void init (FrameHeader header, ClickHandler onGoHome)
    {
        _bottomContent = new SimplePanel();
        super.init(header, onGoHome);
    }

    @Override
    public void setTitleBar (TitleBar bar)
    {
        super.setTitleBar(bar);
        _extendedTitleBar = bar.isExtended();
    }

    @Override // from Layout
    public void setContent (TitleBar bar, Widget content)
    {
        super.setContent(bar, content);
        _bottomContent.setWidget(null);
    }

    @Override // from Layout
    public void closeContent (boolean restoreClient)
    {
        super.closeContent(restoreClient);
        _bottomContent.setWidget(null);
    }

    @Override // from Layout
    public void setBottomContent (Widget content)
    {
        content.setWidth("100%");
        content.setHeight(BOTTOM_FRAME_HEIGHT + "px");
        content.getElement().setAttribute("scrolling", "no");
        _bottomContent.setWidget(content);
        updateMainContentHeight();
    }

    @Override // from Layout
    public void updateTitleBarHeight ()
    {
        updateMainContentHeight();
    }

    @Override // from FramedLayout
    protected void addPanels (RootPanel root)
    {
        super.addPanels(root);
        root.add(_bottomContent);
    }

    @Override // from FramedLayout
    protected int calcMainContentHeight ()
    {
        int height = super.calcMainContentHeight();
        if (_bottomContent.getWidget() != null) {
            height -= BOTTOM_FRAME_HEIGHT;
        }
        return height;
    }

    @Override // from FramedLayout
    protected int getTitleBarHeight ()
    {
        // values also specified in frame.css
        return super.getTitleBarHeight() + (_extendedTitleBar ? 13 : 0);
    }

    protected SimplePanel _bottomContent;
    protected boolean _extendedTitleBar;
    protected static final int BOTTOM_FRAME_HEIGHT = 193;
}
