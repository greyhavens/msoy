//
// $Id$

package client.profile;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Panel;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.web.client.ProfileService;

import client.game.TrophyGrid;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;

/**
 * Displays a member's recently earned trophies.
 */
public class TrophiesBlurb extends Blurb
{
    // @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.trophies != null && pdata.trophies.size() > 0);
    }

    // @Override // from Blurb
    protected Panel createContent ()
    {
        _content = new FlexTable();
        _content.setWidth("100%");
        _content.addStyleName("trophiesBlurb");
        return _content;
    }

    // @Override // from Blurb
    protected void didInit (ProfileService.ProfileResult pdata)
    {
        setHeader(CProfile.msgs.trophiesTitle());

        // display our trophies in a nice grid
        Trophy[] tvec = (Trophy[])pdata.trophies.toArray(new Trophy[pdata.trophies.size()]);
        TrophyGrid.populateTrophyGrid(_content, tvec);

        int row = _content.getRowCount();
        _content.getFlexCellFormatter().setColSpan(row, 0, TrophyGrid.COLUMNS);
        _content.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        _content.getFlexCellFormatter().setStyleName(row, 0, "tipLabel");
        String args = Args.compose("t", pdata.name.getMemberId());
        _content.setWidget(row, 0, Application.createLink(CProfile.msgs.seeAll(), Page.GAME, args));
    }

    protected FlexTable _content;

    protected static final int COLUMNS = 3;
}
