//
// $Id$

package client.frame;

import client.ui.MsoyUI;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

public class FacebookRoomsLayout extends FacebookLayout
{
    @Override
    public void setContent (TitleBar bar, Widget content)
    {
        Widget close = toCloseBox(bar);
        content.setWidth("100%");
        super.setContent(bar, close == null ?
            MsoyUI.createFlowPanel(null, content) :
            MsoyUI.createFlowPanel(null, close, content));
    }

    @Override
    public void setTitleBar (TitleBar bar)
    {
        FlowPanel content = (FlowPanel)_content.getWidget();
        if (content != null) {
            if (content.getWidgetCount() == 2) {
                content.remove(0);
            }
            Widget close = toCloseBox(bar);
            if (close != null) {
                content.insert(close, 0);
            }
        }
        super.setTitleBar(bar);
    }

    @Override
    protected void updateMainContentHeight ()
    {
        super.updateMainContentHeight();

        FlowPanel content = (FlowPanel)_content.getWidget();
        if (content != null) {
            Widget realContent = content.getWidget(content.getWidgetCount() - 1);
            realContent.setHeight(calcMainContentHeight() + "px");
        }
    }

    protected Widget toCloseBox (TitleBar bar)
    {
        return bar != null ? bar.createCloseBox() : null;
    }
}
