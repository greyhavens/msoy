//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.item.ItemMessages;
import client.ui.MsoyUI;
import client.ui.ThumbBox;

/**
 * Displays the current most popular avatars
 */
public class AvatarPanel extends FlowPanel
{
    public AvatarPanel ()
    {
        setStyleName("AvatarPanel");
    }

    /**
     * Display the four most popular avatars
     */
    protected void setAvatars(ListingCard[] cards)
    {
        for (int i = 0; i < 4; i++) {
            if (i >= cards.length) {
                return;
            }
            AvatarBox box = new AvatarBox(cards[i]);
            add(box);
        }
    }

    /**
     * Widget that displays an avatar
     */
    protected static class AvatarBox extends SmartTable
    {
        public AvatarBox (ListingCard card) {
            super("AvatarBox", 0, 0);

            String args = Args.compose("l", "" + card.itemType, "" + card.catalogId);
            setWidget(0, 0, new ThumbBox(card.thumbMedia, MediaDesc.THUMBNAIL_SIZE,
                                         Pages.SHOP, args), 1, "Thumb");
            getFlexCellFormatter().setRowSpan(0, 0, 2);
            setWidget(0, 1, MsoyUI.createLabel(card.name, "Name"));
            setWidget(1, 0, MsoyUI.createLabel(_imsgs.itemBy(card.creator.toString()), "Creator"));
        }
    }

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
}
