//
// $Id$

package client.frame;

import com.google.gwt.user.client.ui.RootPanel;

public class FacebookLayout extends FramedLayout
{
    @Override // from FramedLayout
    public boolean alwaysShowsTitleBar ()
    {
        return true;
    }

    @Override
    public void setTitleBar (TitleBar bar)
    {
        _bar.setWidget(bar.exposeWidget());
    }

    @Override // from FramedLayout
    protected void addPanels (RootPanel root)
    {
        root.add(_bar);
        root.add(_client);
        root.add(_content);
    }

    @Override // from FramedLayout
    protected int getTitleBarHeight ()
    {
        // values also specified in frame.css
        return 53;
    }
}
