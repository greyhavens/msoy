//
// $Id$

package client.ui;

import java.util.Collections;
import java.util.List;

import com.google.gwt.user.client.Random;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

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
        this(Collections.singletonList(promo));
    }

    public PromotionBox (List<Promotion> promos)
    {
        super(MEDIUM_BLUE);
        addStyleName("promoBox");
        _promos = promos;
        showPromotion(Random.nextInt(_promos.size()));
    }

    protected void showPromotion (final int index)
    {
        Promotion promo = _promos.get(index);
        clear();
        HorizontalPanel hbox = new HorizontalPanel();
        hbox.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        if (promo.icon != null) {
            hbox.add(MediaUtil.createMediaView(promo.icon, MediaDesc.HALF_THUMBNAIL_SIZE));
            hbox.add(WidgetUtil.makeShim(10, 10));
        }
        Widget blurb = MsoyUI.createHTML(promo.blurb, "inline");
        hbox.add(blurb);
        hbox.setCellWidth(blurb, "100%");
        if (_promos.size() > 1) {
            hbox.add(WidgetUtil.makeShim(5, 5));
            hbox.add(MsoyUI.createActionImage("/images/ui/promo_arrow.png", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    showPromotion((index + _promos.size()-1) % _promos.size());
                }
            }));
        }
        add(hbox);
    }

    protected List<Promotion> _promos;
}
