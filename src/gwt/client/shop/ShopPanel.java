//
// $Id$

package client.shop;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.ListingCard;
import com.threerings.msoy.web.data.ShopData;

import client.item.ItemRating;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.RoundBox;
import client.util.ThumbBox;

/**
 * Displays the main catalog landing page.
 */
public class ShopPanel extends HorizontalPanel
{
    public ShopPanel ()
    {
        setStyleName("shopPanel");
        setVerticalAlignment(HasAlignment.ALIGN_TOP);

        add(new SideBar(Item.NOT_A_TYPE, null));
        add(WidgetUtil.makeShim(10, 10));
        add(_contents = new FlowPanel());
        add(WidgetUtil.makeShim(10, 10));

        SmartTable header = new SmartTable(0, 0);
        header.setWidget(0, 0, new Image("/images/shop/shop_bag.png"), 1, "Bag");
        header.getFlexCellFormatter().setRowSpan(0, 0, 2);
        header.setText(0, 1, CShop.msgs.shopMarquee(), 1, "Marquee");
        header.setText(1, 0, CShop.msgs.shopIntro(), 1, "Intro");
        _contents.add(header);
        _contents.add(WidgetUtil.makeShim(10, 10));

        // now load up our shop data
        CShop.catalogsvc.loadShopData(CShop.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((ShopData)result);
            }
        });
    }

    protected void init (ShopData data)
    {
        SmartTable boxes = new SmartTable(0, 0);
        boxes.setWidget(0, 0, createTop(null, CShop.msgs.shopTopAvatars(), data.topAvatars));
        boxes.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        boxes.getFlexCellFormatter().setRowSpan(0, 0, 3);
        boxes.setWidget(0, 1, WidgetUtil.makeShim(10, 10));
        boxes.getFlexCellFormatter().setRowSpan(0, 1, 3);
        if (data.featuredPet != null) {
            boxes.setWidget(0, 2, createFeatured(null, CShop.msgs.shopFeatPet(), data.featuredPet));
        }
        boxes.setWidget(1, 0, WidgetUtil.makeShim(10, 10));
        if (data.featuredToy != null) {
            boxes.setWidget(2, 0, createFeatured(null, CShop.msgs.shopFeatToy(), data.featuredToy));
        }
        boxes.setWidget(0, 3, WidgetUtil.makeShim(10, 10));
        boxes.getFlexCellFormatter().setRowSpan(0, 3, 3);
        boxes.setWidget(0, 4, createTop(null, CShop.msgs.shopTopFurniture(), data.topFurniture));
        boxes.getFlexCellFormatter().setVerticalAlignment(0, 4, HasAlignment.ALIGN_TOP);
        boxes.getFlexCellFormatter().setRowSpan(0, 4, 3);

        _contents.add(boxes);
    }

    protected SmartTable createTop (String icon, String title, ListingCard[] listings)
    {
        SmartTable box = MsoyUI.createHeaderBox(null, title);
        for (int ii = 0; ii < listings.length; ii++) {
            box.addWidget(new TopListingBox(ii+1, listings[ii]), 3, "Contents");
        }
        RoundBox.makeRoundBottom(box);
        return box;
    }

    protected SmartTable createFeatured (String icon, String title, ListingCard card)
    {
        FlowPanel left = new FlowPanel();
        left.add(MsoyUI.createLabel(card.name, "Name"));
        left.add(MsoyUI.createLabel(CShop.msgs.itemBy(card.creator.toString()), "Creator"));
        left.add(WidgetUtil.makeShim(5, 5));
        left.add(MsoyUI.createLabel(card.descrip, "Descrip"));

        ClickListener onClick = makeClick(card);
        FlowPanel right = new FlowPanel();
        right.add(new ThumbBox(card.getThumbnailMedia(), onClick));
        right.add(WidgetUtil.makeShim(10, 10));
        right.add(MsoyUI.createButton(MsoyUI.SHORT_THIN, CShop.msgs.shopBuy(), onClick));

        SmartTable contents = new SmartTable("FeatListingBox", 0, 0);
        contents.setWidget(0, 0, left);
        contents.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        contents.setWidget(0, 1, right);
        contents.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
        contents.setWidget(1, 0, WidgetUtil.makeShim(5, 5), 2, null);
        contents.setWidget(2, 0, new PriceLabel(card.flowCost, card.goldCost));
        contents.setWidget(2, 1, ItemRating.createStars(card.rating, false));

        SmartTable box = MsoyUI.createHeaderBox(icon, title, contents);
        RoundBox.makeRoundBottom(box);
        return box;
    }

    protected static ClickListener makeClick (ListingCard card)
    {
        return Application.createLinkListener(
            Page.SHOP, Args.compose("l", "" + card.itemType, "" + card.catalogId));
    }

    protected static class TopListingBox extends SmartTable
    {
        public TopListingBox (int rank, ListingCard card) {
            super("TopListingBox", 0, 0);
            setWidget(0, 1, new ThumbBox(card.getThumbnailMedia(), MediaDesc.HALF_THUMBNAIL_SIZE,
                                         makeClick(card)), 1, "Thumb");
            setText(1, 0, CShop.msgs.shopRank(""+rank), 1, "Ranking");
            setText(1, 1, card.name, 1, "Name");
            setText(2, 1, CShop.msgs.itemBy(card.creator.toString()), 1, "Creator");
        }
    }

    protected FlowPanel _contents;
}
