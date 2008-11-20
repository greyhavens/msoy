//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.HorizontalPanel;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.web.gwt.Promotion;

import client.util.MediaUtil;

/**
 * Displays a promotion.
 */
public class PromotionBox extends RoundBox
{
    public PromotionBox (Promotion promo)
    {
        super(BLUE);
        addStyleName("promoBox");

        if (promo.icon != null) {
            HorizontalPanel hbox = new HorizontalPanel();
            hbox.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            hbox.add(MediaUtil.createMediaView(promo.icon, MediaDesc.HALF_THUMBNAIL_SIZE));
            hbox.add(WidgetUtil.makeShim(10, 10));
            hbox.add(MsoyUI.createHTML(promo.blurb, "inline"));
            add(hbox);
        } else {
            add(MsoyUI.createHTML(promo.blurb, null));
        }
    }
}
