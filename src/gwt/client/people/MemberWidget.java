//
// $Id$

package client.people;

import com.google.gwt.user.client.ui.HasAlignment;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MediaDescSize;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MemberStatusLabel;
import client.util.Link;
import client.util.MediaUtil;

/**
 * Displays a list of members.
 */
public class MemberWidget extends SmartTable
{
    public MemberWidget (MemberCard card)
    {
        super("memberWidget", 0, 5);

        setWidget(0, 0, MediaUtil.createMediaView(card.photo, MediaDescSize.THUMBNAIL_SIZE,
                                                  Link.createHandler(
                                                  Pages.PEOPLE, "" + card.name.getId())),
                  1, "Photo");
        getFlexCellFormatter().setRowSpan(0, 0, 3);

        setWidget(0, 1, Link.create(card.name.toString(), Pages.PEOPLE,
                                               ""+card.name.getId()), 1, "Name");

        // we'll overwrite these below if we have anything to display
        getFlexCellFormatter().setStyleName(1, 0, "Status");
        setHTML(1, 0, "&nbsp;");
        setHTML(2, 0, "&nbsp;");
        if (card.headline != null && card.headline.length() > 0) {
            setText(1, 0, card.headline);
        }
        setWidget(2, 0, new MemberStatusLabel(card));

        SmartTable extras = new SmartTable("Extras", 0, 5);
        addExtras(extras, card);
        setWidget(0, 2, extras);
        getFlexCellFormatter().setRowSpan(0, 2, getRowCount());
        getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
        getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
    }

    protected void addExtras (SmartTable extras, MemberCard card)
    {
        // nothing, presently
    }
}
