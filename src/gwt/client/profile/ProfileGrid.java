//
// $Id$

package client.profile;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.person.data.ProfileCard;
import com.threerings.msoy.web.data.MemberCard;

import client.shell.Application;
import client.shell.Page;
import client.util.MediaUtil;
import client.util.MsoyUI;

/**
 * A grid of profiles.  This is useful for profile searching, or displaying collections of profiles
 * on other pages, such as group page or friends lists.
 */
public class ProfileGrid extends PagedGrid
{
    public ProfileGrid (int height, int width, int navLoc, String emptyMessage) 
    {
        super(height, width, navLoc);
        setEmptyMessage(emptyMessage);
    }

    public void setVerticalOrienation (boolean vertical)
    {
        _vertical = vertical;
    }

    public void setEmptyMessage (String message)
    {
        _emptyMessage = message;
    }

    // @Override // from PagedGrid
    protected Widget createWidget (Object item)
    {
        return new ProfileWidget((MemberCard) item);
    }

    // @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return _emptyMessage;
    }

    // @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return items > (_rows * _cols);
    }

    protected class ProfileWidget extends FlexTable
    {
        public ProfileWidget (final MemberCard card) 
        {
            setStyleName("profileWidget");
            setCellPadding(0);

            ClickListener profileClick = new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.PROFILE, "" + card.name.getMemberId());
                }
            };

            Widget photo = MediaUtil.createMediaView(
                card.photo, MediaDesc.THUMBNAIL_SIZE, profileClick);
            Label nameLabel =  MsoyUI.createActionLabel(card.name.toString(), "Name", profileClick);

            if (_vertical) {
                setCellSpacing(0);

                // avoid forcing this table column to 80 pixels which booches vertical layout mode
                SimplePanel photoPanel = new SimplePanel();
                photoPanel.add(photo);
                photoPanel.setStyleName("Photo");
                setWidget(0, 0, photoPanel);

                setWidget(1, 0, nameLabel);
                getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
                getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_CENTER);

            } else {
                setCellSpacing(5);
                nameLabel.addStyleName("HorizName");

                setWidget(0, 0, photo);
                getFlexCellFormatter().setStyleName(0, 0, "Photo");
                getFlexCellFormatter().setRowSpan(0, 0, 2);

                setWidget(0, 1, nameLabel);
                getFlexCellFormatter().setStyleName(1, 0, "Headline");
                String headline = (card instanceof ProfileCard) ? ((ProfileCard)card).headline : "";
                if (headline == null || headline.length() == 0) {
                    setHTML(1, 0, "&nbsp;");
                } else {
                    setText(1, 0, headline);
                }
            }
        }
    }

    protected String _emptyMessage;
    protected boolean _vertical;
}
