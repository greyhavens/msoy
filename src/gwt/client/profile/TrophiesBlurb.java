//
// $Id$

package client.profile;

import java.util.Iterator;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.client.ProfileService;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MediaUtil;
import client.util.MsoyUI;

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

        int row = 0, col = 0;
        for (Iterator iter = pdata.trophies.iterator(); iter.hasNext(); ) {
            final Trophy trophy = (Trophy) iter.next();
            ClickListener trophyClick = new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.GAME, Args.compose(new String[] {
                        "d", "" + trophy.gameId, "t" }));
                }
            };

            Widget thumb = MediaUtil.createMediaView(
                trophy.trophyMedia, MediaDesc.HALF_THUMBNAIL_SIZE);
            if (thumb instanceof Image) {
                ((Image) thumb).addClickListener(trophyClick);
                thumb.setStyleName("actionLabel");
            }
            _content.setWidget(row, col, thumb);
            _content.getFlexCellFormatter().setStyleName(row, col, "Image");

            _content.setWidget(row+1, col, MsoyUI.createActionLabel(trophy.name, trophyClick));
            _content.getFlexCellFormatter().setStyleName(row+1, col, "Name");

            if (++col == COLUMNS) {
                row += 2;
                col = 0;
            }
        }

        // if they have only one or two trophies total, we need to add blank cells to ensure that
        // things are only as wide as appropriate
        for (int ii = pdata.trophies.size(); ii < 3; ii++) {
            _content.setText(0, ii, "");
            _content.getFlexCellFormatter().setStyleName(0, ii, "Image");
        }

        row = _content.getRowCount();
        _content.getFlexCellFormatter().setColSpan(row, 0, COLUMNS);
        _content.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_CENTER);
        _content.getFlexCellFormatter().setStyleName(row, 0, "More");
        String args = Args.compose("t", pdata.name.getMemberId());
        _content.setWidget(row, 0, Application.createLink(
                               CProfile.msgs.allTrophies(), "game", args));
    }

    protected FlexTable _content;

    protected static final int COLUMNS = 3;
}
