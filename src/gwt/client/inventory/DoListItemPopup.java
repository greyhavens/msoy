//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.data.gwt.CatalogListing;

import client.util.BorderedDialog;
import client.util.ClickCallback;
import client.util.MsoyUI;
import client.util.NumberTextBox;

public class DoListItemPopup extends BorderedDialog
{
    public DoListItemPopup (Item item, final CatalogListing listing)
    {
        super(false, true);
        addStyleName("doListItem");
        _item = item;

        _status = new Label("");
        _status.addStyleName("Status");

        // note whether we are listing this item for the first time or updating its listing or
        // whether or not we're repricing an existing listing
        boolean firstTime = (item.catalogId == 0), repricing = (listing != null);

        _header.add(createTitleLabel(CInventory.msgs.doListHdrTop(), null));
        if (firstTime) {
            _content.add(MsoyUI.createLabel(CInventory.msgs.doListBlurb(), "Blurb"));
        } else if (repricing) {
            _content.add(MsoyUI.createLabel(CInventory.msgs.doUppriceBlurb(), "Blurb"));
        } else {
            _content.add(MsoyUI.createLabel(CInventory.msgs.doUpdateBlurb(), "Blurb"));
        }

        // determine whether or not this item is salable
        boolean salableItem = !(_item instanceof SubItem) || ((SubItem)_item).isSalable();

        // only add the description if we're not repricing
        if (!repricing) {
            _content.add(MsoyUI.createLabel(CInventory.msgs.doListDescripHeader(), "Header"));
            _content.add(MsoyUI.createLabel(firstTime ? CInventory.msgs.doListNeedsDescrip() :
                                            CInventory.msgs.doUpdateNeedsDescrip(), "Blurb"));
            _content.add(_description = new TextArea());
            _description.setText(item.description);
            _description.setCharacterWidth(50);
            _description.setVisibleLines(3);
        }

        // possibly add the pricing selection UI
        if (salableItem && (firstTime || repricing)) {
            SmartTable pricing = new SmartTable();
            pricing.setCellPadding(0);
            pricing.setCellSpacing(3);

            int row = pricing.addText(CInventory.msgs.doListStrategy(), 1, "rightLabel");
            pricing.setWidget(row, 1, _pricingBox = new ListBox(), 1, null);
            int selectedPricing = (_item instanceof SubItem) ? 0 /* hidden */ : 1 /* manual */;
            for (int ii = 0; ii < CatalogListing.PRICING.length; ii++) {
                String key = "listingPricing" + CatalogListing.PRICING[ii];
                _pricingBox.addItem(CInventory.dmsgs.getString(key));
                if (listing != null && listing.pricing == CatalogListing.PRICING[ii]) {
                    selectedPricing = ii;
                }
            }
            ChangeListener tipper = new ChangeListener() {
                public void onChange (Widget sender) {
                    int pricing = getPricing();
                    _pricingTip.setText(CInventory.dmsgs.getString("listingPricingTip" + pricing));
                    boolean showSalesTarget = (pricing == CatalogListing.PRICING_ESCALATE ||
                                               pricing == CatalogListing.PRICING_LIMITED_EDITION);
                    _salesTargetLabel.setVisible(showSalesTarget);
                    _salesTarget.setVisible(showSalesTarget);
                }
            };
            _pricingBox.addChangeListener(tipper);

            pricing.setWidget(row, 2, _pricingTip = new Label(""), 2, "Blurb");
            pricing.getFlexCellFormatter().setRowSpan(row, 2, 3);

            _salesTargetLabel = new Label(CInventory.msgs.doListSalesTarget());
            row = pricing.addWidget(_salesTargetLabel, 1, "rightLabel");
            pricing.setWidget(row, 1, _salesTarget = new NumberTextBox(false, 5, 5), 1, null);
            int salesTarget = (listing == null) ? DEFAULT_SALES_TARGET : listing.salesTarget;
            _salesTarget.setText(String.valueOf(salesTarget));

            row = pricing.addText(CInventory.msgs.doListFlowCost(), 1, "rightLabel");
            pricing.setWidget(row, 1, _flowCost = new NumberTextBox(false, 5, 5), 1, null);
            int flowCost = (listing == null) ? DEFAULT_FLOW_COST : listing.flowCost;
            _flowCost.setText(String.valueOf(flowCost));

//             row = pricing.addText(CInventory.msgs.doListGoldCost(), 1, "rightLabel");
//             pricing.setWidget(row, 1, _goldCost = new NumberTextBox(false, 5, 5), 2, null);
//             int goldCost = (listing == null) ? DEFAULT_GOLD_COST : listing.goldCost;
//             _goldCost.setText(String.valueOf(goldCost));

            _content.add(MsoyUI.createLabel(CInventory.msgs.doListPricingHeader(), "Header"));
            _content.add(pricing);

            _pricingBox.setSelectedIndex(selectedPricing);
            tipper.onChange(_pricingBox); // alas setSelectedIndex() doesn't do this, yay DHTML
        }

        _content.add(_status);

        _footer.add(new Button(CInventory.msgs.doListBtnCancel(), new ClickListener() {
            public void onClick (Widget sender) {
                hide();
            }
        }));

        String doLbl = firstTime ? CInventory.msgs.doListBtnGo() : CInventory.msgs.doUpdateBtnGo();
        _footer.add(_doIt = new Button(doLbl));

        if (firstTime) {
            final String resultMsg = firstTime ?
                CInventory.msgs.doListListed() : CInventory.msgs.doListUpdated();
            new ClickCallback(_doIt) {
                public boolean callService () {
                    CInventory.catalogsvc.listItem(
                        CInventory.ident, _item.getIdent(), _description.getText(), getPricing(),
                        getSalesTarget(), getFlowCost(), 0 /* getGoldCost() */, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    // TODO: enhance dialog to link to catalog page to see item
                    MsoyUI.info(resultMsg);
                    hide();
                    return false;
                }
            };

        } else if (repricing) {
            new ClickCallback(_doIt) {
                public boolean callService () {
                    int pricing = getPricing(), salesTarget = getSalesTarget();
                    if (getPricing() == CatalogListing.PRICING_LIMITED_EDITION &&
                        listing != null && salesTarget <= listing.purchases) {
                        MsoyUI.error(CInventory.msgs.doListHitLimit(""+listing.purchases));
                        return false;
                    }
                    CInventory.catalogsvc.updatePricing(
                        CInventory.ident, _item.getType(), _item.catalogId, getPricing(),
                        salesTarget, getFlowCost(), 0 /* getGoldCost() */, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    MsoyUI.info(CInventory.msgs.doListUpdated());
                    hide();
                    return false;
                }
            };

        } else {
            new ClickCallback(_doIt) {
                public boolean callService () {
                    CInventory.catalogsvc.updateListing(
                        CInventory.ident, _item.getIdent(), _description.getText(), this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    MsoyUI.info(CInventory.msgs.doListUpdated());
                    hide();
                    return false;
                }
            };
        }
    }

    protected int getPricing ()
    {
        if (_pricingBox == null) {
            // non-salable items have no pricing interface and are always HIDDEN
            return CatalogListing.PRICING[0];
        } else {
            return CatalogListing.PRICING[Math.max(0, _pricingBox.getSelectedIndex())];
        }
    }

    protected int getSalesTarget ()
    {
        // non-salable items have no pricing interface and a default sales target
        return (_salesTarget == null) ? 100 : _salesTarget.getValue().intValue();
    }

    protected int getFlowCost ()
    {
        // non-salable items have no pricing interface and a default flow cost
        return (_flowCost == null) ? 100 : _flowCost.getValue().intValue();
    }

    protected int getGoldCost ()
    {
        // non-salable items have no pricing interface and a default gold cost
        return (_goldCost == null) ? 0 : _goldCost.getValue().intValue();
    }

    // @Override
    protected Widget createContents ()
    {
        _content = new VerticalPanel();
        _content.addStyleName("Content");
        _content.setVerticalAlignment(HasAlignment.ALIGN_TOP);
        return _content;
    }

    protected Item _item;

    protected VerticalPanel _content;
    protected TextArea _description;
    protected ListBox _pricingBox;
    protected Label _pricingTip, _salesTargetLabel;
    protected NumberTextBox _salesTarget, _flowCost, _goldCost;

    protected Label _status;
    protected Button _doIt;

    protected static final int DEFAULT_FLOW_COST = 100;
    protected static final int DEFAULT_GOLD_COST = 0;
    protected static final int DEFAULT_SALES_TARGET = 500;
}
