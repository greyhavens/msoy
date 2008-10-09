//
// $Id$

package client.item;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;

import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.ui.NumberTextBox;
import client.util.ClickCallback;
import client.util.ServiceUtil;

public class DoListItemPopup extends VerticalPanel
{
    public interface ListedListener
    {
        void itemListed (Item item, boolean updated);
    }

    public static void show (Item item, CatalogListing listing, ListedListener listener)
    {
        String title = (item.catalogId == 0) ? _imsgs.doListCreateTitle()
                                             : _imsgs.doListUpdateTitle();
        CShell.frame.showDialog(title, new DoListItemPopup(item, listing, listener));
    }

    protected DoListItemPopup (Item item, final CatalogListing listing, ListedListener listener)
    {
        addStyleName("doListItem");

        _item = item;
        _listener = listener;

        _status = new Label("");
        _status.addStyleName("Status");

        // determine whether or not this item is salable
        boolean salableItem = !(_item instanceof SubItem) || ((SubItem)_item).isSalable();

        // note whether we are listing this item for the first time or updating its listing or
        // whether or not we're repricing an existing listing
        boolean firstTime = (item.catalogId == 0), repricing = (listing != null);
        if (firstTime) {
            add(MsoyUI.createLabel(_imsgs.doListBlurb(), "Blurb"));
        } else if (repricing) {
            add(MsoyUI.createLabel(_imsgs.doUppriceBlurb(), "Blurb"));
        } else {
            String message = _imsgs.doUpdateBlurb();
            if (salableItem) {
                message += _imsgs.doUpdateSalableNote();
            }
            add(MsoyUI.createHTML(message, "Blurb"));
        }

        // only add the description if we're not repricing
        if (!repricing) {
            add(MsoyUI.createLabel(_imsgs.doListDescripHeader(), "Header"));
            add(MsoyUI.createLabel(firstTime ? _imsgs.doListNeedsDescrip() :
                                   _imsgs.doUpdateNeedsDescrip(), "Blurb"));
            add(_description = new TextArea());
            _description.setText(item.description);
            _description.setCharacterWidth(50);
            _description.setVisibleLines(3);
        }

        // possibly add the pricing selection UI
        if (salableItem && (firstTime || repricing)) {
            SmartTable pricing = new SmartTable();
            pricing.setCellPadding(0);
            pricing.setCellSpacing(3);

            int row = pricing.addText(_imsgs.doListStrategy(), 1, "rightLabel");
            pricing.setWidget(row, 1, _pricingBox = new ListBox(), 1, null);
            int selectedPricingIndex = (_item instanceof SubItem && !((SubItem) _item).isSalable())
                ? 0 /* hidden */ : 1 /* manual */;
            for (int ii = 0; ii < CatalogListing.PRICING.length; ii++) {
                String key = "listingPricing" + CatalogListing.PRICING[ii];
                _pricingBox.addItem(_dmsgs.xlate(key));
                if (listing != null && listing.pricing == CatalogListing.PRICING[ii]) {
                    selectedPricingIndex = ii;
                }
            }
            ChangeListener tipper = new ChangeListener() {
                public void onChange (Widget sender) {
                    int pricing = getPricing();
                    _pricingTip.setText(_dmsgs.xlate("listingPricingTip" + pricing));
                    boolean showSalesTarget = (pricing == CatalogListing.PRICING_ESCALATE ||
                                               pricing == CatalogListing.PRICING_LIMITED_EDITION);
                    _salesTargetLabel.setVisible(showSalesTarget);
                    _salesTarget.setVisible(showSalesTarget);
                }
            };
            _pricingBox.addChangeListener(tipper);

            pricing.setWidget(row, 2, _pricingTip = new Label(""), 2, "Blurb");
            pricing.getFlexCellFormatter().setRowSpan(row, 2, 2);

            _salesTargetLabel = new Label(_imsgs.doListSalesTarget());
            row = pricing.addWidget(_salesTargetLabel, 1, "rightLabel");
            pricing.setWidget(row, 1, _salesTarget = new NumberTextBox(false, 5, 5), 1, null);
            int salesTarget = (listing == null) ? DEFAULT_SALES_TARGET : listing.salesTarget;
            _salesTarget.setText(String.valueOf(salesTarget));

            if (CShell.barsEnabled()) {
                _currencyBox = new ListBox();
                for (int i=0; i<LISTABLE_CURRENCIES.length; ++i) {
                    _currencyBox.addItem(_dmsgs.xlate(LISTABLE_CURRENCIES[i].getLabel()));
                    if (listing != null &&
                        LISTABLE_CURRENCIES[i] == listing.quote.getListedCurrency()) {
                        _currencyBox.setSelectedIndex(i);
                    }
                }
                row = pricing.addWidget(new Label(_imsgs.doListCurrency()), 1, "rightLabel");
                pricing.setWidget(row, 1, _currencyBox);
                pricing.setWidget(row, 2, new Label(_imsgs.doListCurrencyTip()), 1, "Blurb");
                pricing.getFlexCellFormatter().setRowSpan(row, 2, 2);
            }

            row = pricing.addText(_imsgs.doListCost(), 1, "rightLabel");
            pricing.setWidget(row, 1, _cost = new NumberTextBox(false, 5, 5), 1, null);
            int cost = (listing == null) ? DEFAULT_COIN_COST : listing.quote.getListedAmount();
            _cost.setText(String.valueOf(cost));

            add(MsoyUI.createLabel(_imsgs.doListPricingHeader(), "Header"));
            add(pricing);

            _pricingBox.setSelectedIndex(selectedPricingIndex);
            tipper.onChange(_pricingBox); // alas setSelectedIndex() doesn't do this, yay DHTML
        }

        add(_status);

        // create buttons for listing and
        HorizontalPanel footer = new HorizontalPanel();
        footer.add(new Button(_imsgs.doListBtnCancel(), new ClickListener() {
            public void onClick (Widget sender) {
                CShell.frame.clearDialog();
            }
        }));
        footer.add(WidgetUtil.makeShim(10, 10));
        String doLbl = firstTime ? _imsgs.doListBtnGo() : _imsgs.doUpdateBtnGo();
        footer.add(_doIt = new Button(doLbl));
        setHorizontalAlignment(ALIGN_RIGHT);
        add(footer);

        // set up our button actions
        if (firstTime) {
            final String resultMsg = firstTime ? _imsgs.doListListed() : _imsgs.doListUpdated();
            new ClickCallback<Integer>(_doIt) {
                public boolean callService () {
                    int cost = getCost();
                    Currency currency = (cost == 0) ? Currency.COINS : getCurrency();
                    _catalogsvc.listItem(_item.getIdent(), _description.getText(), getPricing(),
                                         getSalesTarget(), currency, cost, this);
                    return true;
                }
                public boolean gotResult (Integer result) {
                    _item.catalogId = result;
                    MsoyUI.info(resultMsg);
                    CShell.frame.clearDialog();
                    _listener.itemListed(_item, false);
                    return false;
                }
            };

        } else if (repricing) {
            new ClickCallback<Void>(_doIt) {
                public boolean callService () {
                    int pricing = getPricing(), salesTarget = getSalesTarget();
                    if (pricing == CatalogListing.PRICING_LIMITED_EDITION &&
                            listing != null && salesTarget <= listing.purchases) {
                        MsoyUI.error(_imsgs.doListHitLimit(""+listing.purchases));
                        return false;
                    }
                    _catalogsvc.updatePricing(_item.getType(), _item.catalogId, pricing,
                                              salesTarget, getCurrency(), getCost(), this);
                    return true;
                }
                public boolean gotResult (Void result) {
                    MsoyUI.info(_imsgs.doListUpdated());
                    CShell.frame.clearDialog();
                    _listener.itemListed(_item, true);
                    return false;
                }
            };

        } else {
            new ClickCallback<Void>(_doIt) {
                public boolean callService () {
                    _catalogsvc.updateListing(_item.getIdent(), _description.getText(), this);
                    return true;
                }
                public boolean gotResult (Void result) {
                    MsoyUI.info(_imsgs.doListUpdated());
                    CShell.frame.clearDialog();
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

    protected Currency getCurrency ()
    {
        return (_currencyBox == null) ? Currency.COINS :
            LISTABLE_CURRENCIES[_currencyBox.getSelectedIndex()];
    }

    protected int getCost ()
    {
        // non-salable items have no pricing interface and a default flow cost
        return (_cost == null) ? 100 : _cost.getValue().intValue();
    }

    protected Item _item;
    protected ListedListener _listener;

    protected TextArea _description;
    protected ListBox _pricingBox;
    protected ListBox _currencyBox;
    protected Label _pricingTip, _salesTargetLabel;
    protected NumberTextBox _salesTarget, _cost;

    protected Label _status;
    protected Button _doIt;

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);

    protected static final int DEFAULT_COIN_COST = 100;
    protected static final int DEFAULT_SALES_TARGET = 500;

    protected static final Currency[] LISTABLE_CURRENCIES = { Currency.COINS, Currency.BARS };
}
