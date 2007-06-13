//
// $Id$

package client.util;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.MemberCard;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.WidgetUtil;

import client.profile.CProfile;
import client.shell.Application;
import client.util.MediaUtil;

/**
 * A Grid of profiles.  This is useful for profile searching, or displaying collections of profiles
 * on other pages, such as group page or friends lists.
 */
public class ProfileGrid extends PagedGrid
{
    public ProfileGrid (int height, int width) 
    {
        super(height, width);
    }

    protected Widget createWidget (Object item) {
        return new ProfileWidget((MemberCard) item);
    }
    protected String getEmptyMessage () {
        return CProfile.msgs.gridNoProfiles();
    }

    protected class ProfileWidget extends HorizontalPanel
    {
        public ProfileWidget (final MemberCard card) 
        {
            setStyleName("ProfileWidget");
            setSpacing(10);

            ClickListener profileClick = new ClickListener() {
                public void onClick (Widget sender) {
                    History.newItem(Application.createLinkToken("profile", "" + 
                        card.name.getMemberId()));
                }
            };
            Widget photo = MediaUtil.createMediaView(card.photo, MediaDesc.HALF_THUMBNAIL_SIZE);
            if (photo instanceof Image) {
                ((Image) photo).addClickListener(profileClick);
            }
            SimplePanel photoPanel = new SimplePanel();
            photoPanel.add(photo);
            photoPanel.setStyleName("Photo");
            add(photoPanel);

            VerticalPanel text = new VerticalPanel();
            Label nameLabel =  new Label(card.name.toString());
            nameLabel.setStyleName("memberCardName");
            nameLabel.addClickListener(profileClick);
            text.add(nameLabel);
            Label headlineLabel = new Label(card.headline);
            headlineLabel.setStyleName("memberCardHeadline");
            text.add(headlineLabel);
            add(text);
        }
    }
}
