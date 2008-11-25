//
// $Id$

package client.ui;

import java.util.List;

import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
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
        hbox.add(MsoyUI.createHTML(promo.blurb, "inline"));
        if (_promos.size() > 1) {
            hbox.add(WidgetUtil.makeShim(10, 10));
            FlowPanel navi = new FlowPanel();
            navi.add(createActionLink("&larr;", new ClickListener() {
                public void onClick (Widget sender) {
                    showPromotion((index + _promos.size()-1) % _promos.size());
                }
            }));
            navi.add(WidgetUtil.makeShim(5, 5));
            navi.add(createActionLink("&rarr;", new ClickListener() {
                public void onClick (Widget sender) {
                    showPromotion((index + 1) % _promos.size());
                }
            }));
            hbox.add(navi);
        }
        add(hbox);
    }

    protected HTML createActionLink (String glyph, ClickListener listener)
    {
        HTML link = new HTML(glyph);
        link.addClickListener(listener);
        link.addStyleName("hoverActionLabel");
        return link;
    }

    protected List<Promotion> _promos;
}
