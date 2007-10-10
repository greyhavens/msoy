//
// $Id$

package client.profile;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.MemberCard;

import com.threerings.gwt.ui.PagedGrid;
import client.shell.Application;
import client.shell.Page;
import client.util.MediaUtil;

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

    protected Widget createWidget (Object item)
    {
        return new ProfileWidget((MemberCard) item);
    }

    protected String getEmptyMessage ()
    {
        return _emptyMessage;
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

            Widget photo = MediaUtil.createMediaView(card.photo, MediaDesc.HALF_THUMBNAIL_SIZE);
            if (photo instanceof Image) {
                ((Image) photo).addClickListener(profileClick);
                photo.setStyleName("actionLabel");
            }
            // we do this crazy double wrapping to avoid forcing this table column to 80 pixels
            // which booches vertical layout mode
            SimplePanel photoPanel = new SimplePanel();
            photoPanel.add(photo);
            photoPanel.setStyleName("Photo");
            setWidget(0, 0, photoPanel);

            Label nameLabel =  new Label(card.name.toString());
            nameLabel.setStyleName("MemberCardName");
            nameLabel.addClickListener(profileClick);

            if (_vertical) {
                setCellSpacing(0);
                setWidget(1, 0, nameLabel);
            } else {
                setCellSpacing(5);
                getFlexCellFormatter().setRowSpan(0, 0, 2);
                setWidget(0, 1, nameLabel);
                getFlexCellFormatter().setStyleName(1, 0, "MemberCardHeadline");
                setText(1, 0, card.headline);
            }
        }
    }

    protected String _emptyMessage;
    protected boolean _vertical;
}
