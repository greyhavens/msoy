//
// $Id$

package client.item;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.item.gwt.ItemPrices;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MoneyLabel;
import client.ui.MsoyUI;
import client.ui.NumberTextBox;
import client.ui.Stars;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.util.Link;
import client.util.ServiceUtil;

public class DoListItemPopup extends VerticalPanel
{
    public interface ListedListener
    {
        void itemListed (Item item, boolean updated);
    }

    public static void show (Item item, CatalogListing listing, ListedListener listener)
    {
        // if salable, require that the item in question have a thumbnmail image
        if (item.isSalable() && item.getRawThumbnailMedia() == null) {
            MsoyUI.error(_imsgs.doListNeedThumb());
            return;
        }

        String title = (item.catalogId == 0) ? _imsgs.doListCreateTitle()
                                             : _imsgs.doListUpdateTitle();
        CShell.frame.showDialog(title, new DoListItemPopup(item, listing, listener));
    }

    protected DoListItemPopup (Item item, final CatalogListing listing, ListedListener listener)
    {
        addStyleName("doListItem");

        _item = item;
        _listener = listener;

        // note whether we are listing this item for the first time or updating its listing or
        // whether or not we're repricing an existing listing
        boolean firstTime = (item.catalogId == 0), repricing = (listing != null);
        if (firstTime) {
            add(MsoyUI.createLabel(_imsgs.doListBlurb(), "Blurb"));
        } else if (repricing) {
            add(MsoyUI.createLabel(_imsgs.doUppriceBlurb(), "Blurb"));
        } else {
            String message = _imsgs.doUpdateBlurb();
            if (_item.isSalable()) {
                message += _imsgs.doUpdateSalableNote();
            }
            add(MsoyUI.createHTML(message, "Blurb"));
        }

        // add the description (if we're not just repricing)
        if (!repricing) {
            add(MsoyUI.createLabel(_imsgs.doListDescripHeader(), "Header"));
            add(MsoyUI.createLabel(firstTime ? _imsgs.doListNeedsDescrip() :
                                   _imsgs.doUpdateNeedsDescrip(), "Blurb"));
            add(MsoyUI.createLabel(item.description, "Descrip"));
            add(Link.create("Edit description...", Pages.STUFF, "e", item.getType(), item.itemId));
        }

        // add a rating interface
        if (_item.isSalable() && firstTime) {
            SmartTable rating = new SmartTable(0, 3);
            rating.addWidget(MsoyUI.createHTML(_imsgs.doListRatingIntro(), null), 2, null);
            rating.addWidget(WidgetUtil.makeShim(5, 5), 2, null);
            int row = rating.addText(_imsgs.doListRating(), 1, null);
            rating.getFlexCellFormatter().setWidth(row, 0, "150px"); // yay html!
            rating.setWidget(row, 1, _stars, 1, null);
            row = rating.addText(_imsgs.doListFee(), 1, null);
            _fee = new MoneyLabel(Currency.COINS, getMinimumPrice(Currency.COINS));
            rating.setWidget(row, 1, _fee, 1, null);
            add(MsoyUI.createLabel(_imsgs.doListRatingHeader(), "Header"));
            add(rating);

        } else if (firstTime) {
            // it's not a salable item, so the rating doesn't matter, but we need something
            _stars.setRating(5);

        } else if (repricing) {
            _stars.setRating(listing.detail.item.getRating());
        }

        // possibly add the basis selection ui
        if (_item.supportsDerviation() && firstTime) {
            SmartTable basis = new SmartTable(0, 3);
            basis.addWidget(MsoyUI.createHTML(_imsgs.doListSelectBasisIntro(), null), 3, null);
            basis.addWidget(WidgetUtil.makeShim(5, 5), 3, null);

            int row = basis.addText(_imsgs.doListBasis(), 1, "rightLabel");
            basis.setWidget(row, 1, _basisBox = new ListBox(), 1, null);
            _basisBox.addItem(_imsgs.doListNoBasis());
            _basisBox.addChangeHandler(new ChangeHandler() {
                public void onChange (ChangeEvent event) {
                    ListingCard basis = getBasis();
                    _currencyBox.setEnabled(basis == null);
                    if (basis != null) {
                        setCurrency(basis.currency);
                    }
                }
            });

            _catalogsvc.loadPotentialBasisItems(_item.getType(),
                new InfoCallback<List<ListingCard>>() {
                    @Override public void onSuccess (List<ListingCard> result) {
                        gotBasisItems(result);
                    }
            });

            add(MsoyUI.createLabel(_imsgs.doListSelectBasisHeader(), "Header"));
            add(basis);
        }

        // possibly add the pricing selection UI
        if (_item.isSalable() && (firstTime || repricing)) {
            SmartTable pricing = new SmartTable(0, 3);

            int row = pricing.addWidget(
                MsoyUI.createHTML(_imsgs.doListPricingIntro(), null), 3, null);
            pricing.addWidget(WidgetUtil.makeShim(5, 5), 3, null);

            row = pricing.addText(_imsgs.doListStrategy(), 1, "rightLabel");
            pricing.setWidget(row, 1, _pricingBox = new ListBox(), 1, null);
            int defaultPricing = repricing ? listing.pricing : CatalogListing.PRICING_ESCALATE;
            int pricingIndex = -1;
            for (int ii = 0; ii < CatalogListing.PRICING.length; ii++) {
                String key = "listingPricing" + CatalogListing.PRICING[ii];
                _pricingBox.addItem(_dmsgs.xlate(key));
                if (defaultPricing == CatalogListing.PRICING[ii]) {
                    pricingIndex = ii;
                }
            }
            ChangeHandler tipper = new ChangeHandler() {
                public void onChange (ChangeEvent event) {
                    int pricing = getPricing();
                    _pricingTip.setText(_dmsgs.xlate("listingPricingTip" + pricing));
                    boolean showSalesTarget = (pricing == CatalogListing.PRICING_ESCALATE ||
                                               pricing == CatalogListing.PRICING_LIMITED_EDITION);
                    _salesTargetLabel.setVisible(showSalesTarget);
                    _salesTarget.setVisible(showSalesTarget);
                }
            };
            _pricingBox.addChangeHandler(tipper);

            pricing.setWidget(row, 2, _pricingTip = new Label(""), 2, "Blurb");
            pricing.getFlexCellFormatter().setRowSpan(row, 2, 2);

            _salesTargetLabel = new Label(_imsgs.doListSalesTarget());
            row = pricing.addWidget(_salesTargetLabel, 1, "rightLabel");
            pricing.setWidget(row, 1, _salesTarget = new NumberTextBox(false, 5, 5), 1, null);
            int salesTarget = (listing == null) ? DEFAULT_SALES_TARGET : listing.salesTarget;
            _salesTarget.setNumber(salesTarget);

            pricing.addWidget(WidgetUtil.makeShim(5, 5), 3, null);

            _currencyBox = new ListBox();
            for (int i=0; i<LISTABLE_CURRENCIES.length; ++i) {
                _currencyBox.addItem(_dmsgs.xlate(LISTABLE_CURRENCIES[i].getLabel()));
            }
            if (listing != null) {
                setCurrency(listing.quote.getListedCurrency());
                if (listing.basisId > 0) {
                    _currencyBox.setEnabled(false);
                    _catalogsvc.loadListing(item.getType(), listing.basisId, false,
                        new InfoCallback<CatalogListing>() {
                            public void onSuccess (CatalogListing result) {
                                _previousBasis = result;
                            }
                        });
                }
            }
            row = pricing.addWidget(new Label(_imsgs.doListCurrency()), 1, "rightLabel");
            pricing.setWidget(row, 1, _currencyBox);
            pricing.setWidget(row, 2, new Label(_imsgs.doListCurrencyTip()), 1, "Blurb");
            pricing.getFlexCellFormatter().setRowSpan(row, 2, 2);

            row = pricing.addText(_imsgs.doListCost(), 1, "rightLabel");
            pricing.setWidget(row, 1, _cost = new NumberTextBox(false, 5, 5), 1, null);
            int cost = (listing == null) ? DEFAULT_COIN_COST : listing.quote.getListedAmount();
            _cost.setNumber(cost);

            add(MsoyUI.createLabel(_imsgs.doListPricingHeader(), "Header"));
            add(pricing);

            if (pricingIndex != -1) {
                _pricingBox.setSelectedIndex(pricingIndex);
            }
            tipper.onChange(null); // alas setSelectedIndex() doesn't do this, yay DHTML
        }

        // create buttons for listing and
        HorizontalPanel footer = new HorizontalPanel();
        footer.add(new Button(_imsgs.doListBtnCancel(), new ClickHandler() {
            public void onClick (ClickEvent event) {
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
                @Override protected boolean callService () {
                    byte rating = (byte)_stars.getRating();
                    if (rating == 0) {
                        MsoyUI.error(_imsgs.doListNeedRating());
                        return false;
                    }
                    if (!validatePricing()) {
                        return false;
                    }
                    ListingCard basis = getBasis();
                    _catalogsvc.listItem(_item.getIdent(), rating, getPricing(), getSalesTarget(),
                                         getCurrency(), getCost(),
                                         basis == null ? 0 : basis.catalogId, this);
                    return true;
                }
                @Override protected boolean gotResult (Integer result) {
                    _item.catalogId = result;
                    MsoyUI.info(resultMsg);
                    CShell.frame.clearDialog();
                    _listener.itemListed(_item, false);
                    return false;
                }
            };

        } else if (repricing) {
            new ClickCallback<Void>(_doIt, listing.derivationCount > 0 ?
                _imsgs.doListRepriceBasisConfirm("" + listing.derivationCount) : null) {
                @Override protected boolean callService () {
                    int pricing = getPricing(), salesTarget = getSalesTarget();
                    if (pricing == CatalogListing.PRICING_LIMITED_EDITION &&
                            listing != null && salesTarget <= listing.purchases) {
                        MsoyUI.error(_imsgs.doListHitLimit(""+listing.purchases));
                        return false;
                    }
                    if (!validatePricing()) {
                        return false;
                    }
                    _catalogsvc.updatePricing(_item.getType(), _item.catalogId, pricing,
                                              salesTarget, getCurrency(), getCost(), this);
                    return true;
                }
                @Override protected boolean gotResult (Void result) {
                    MsoyUI.info(_imsgs.doListUpdated());
                    CShell.frame.clearDialog();
                    _listener.itemListed(_item, true);
                    return false;
                }
            };

        } else {
            new ClickCallback<Void>(_doIt) {
                @Override protected boolean callService () {
                    _catalogsvc.updateListing(_item.getIdent(), this);
                    return true;
                }
                @Override protected boolean gotResult (Void result) {
                    MsoyUI.info(_imsgs.doListUpdated());
                    CShell.frame.clearDialog();
                    return false;
                }
            };
        }
    }

    protected boolean validatePricing ()
    {
        if (getCost() < getMinimumPrice(getCurrency())) {
            MsoyUI.error(_imsgs.doListMissedMinimum(""+getMinimumPrice(Currency.COINS)));
            return false;
        }
        ListingCard basis = getBasis();
        if (basis != null && !validateDerivedPricing(basis.currency, basis.cost)) {
            return false;
        }
        if (_previousBasis != null && !validateDerivedPricing(
            _previousBasis.quote.getListedCurrency(), _previousBasis.quote.getListedAmount())) {
            return false;
        }
        return true;
    }

    protected boolean validateDerivedPricing (Currency basisCurrency, int basisCost)
    {
        int minCost = CatalogListing.getMinimumDerivedCost(basisCurrency, basisCost);
        if (getCost() < minCost) {
            MsoyUI.error(_imsgs.doListMissedBasisMinimum(String.valueOf(basisCost),
                _dmsgs.xlate(basisCurrency.getKey()), String.valueOf(minCost)));
            return false;
        }
        return true;
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
        return (_salesTarget == null) ? 100 : _salesTarget.getNumber().intValue();
    }

    protected Currency getCurrency ()
    {
        return (_currencyBox == null) ? Currency.COINS :
            LISTABLE_CURRENCIES[_currencyBox.getSelectedIndex()];
    }

    protected int getCost ()
    {
        // non-salable items have no pricing interface and a default flow cost
        return (_cost == null) ? ItemPrices.DEFAULT_MIN_PRICE : _cost.getNumber().intValue();
    }

    protected int getMinimumPrice (Currency currency)
    {
        return ItemPrices.getMinimumPrice(currency, _item.getType(), (byte)_stars.getRating());
    }

    protected ListingCard getBasis()
    {
        int basisSel = _basisBox == null ? 0 : _basisBox.getSelectedIndex();
        return basisSel == 0 ? null : _basisItems.get(basisSel - 1);
    }

    protected void setCurrency (Currency currency)
    {
        if (_currencyBox == null) {
            return;
        }

        for (int ii = 0; ii < LISTABLE_CURRENCIES.length; ++ii) {
            if (LISTABLE_CURRENCIES[ii] == currency) {
                _currencyBox.setSelectedIndex(ii);
                break;
            }
        }
    }

    protected void gotBasisItems (List<ListingCard> items)
    {
        for (ListingCard item : items) {
            _basisBox.addItem(item.name);
            _basisItems.add(item);
        }
    }

    protected Stars _stars = new Stars(0f, false, false, new Stars.StarMouseListener() {
        public void starClicked (byte newRating) {
            _stars.setRating(newRating);
            _fee.update(Currency.COINS, getMinimumPrice(Currency.COINS));
        }
        public void starMouseOn (byte rating) {
            // nada
        }
        public void starMouseOff () {
            // nada
        }
    });

    protected Item _item;
    protected ListedListener _listener;

    protected ListBox _basisBox;
    protected List<ListingCard> _basisItems = new ArrayList<ListingCard>();
    protected CatalogListing _previousBasis;

    protected ListBox _pricingBox;
    protected ListBox _currencyBox;
    protected Label _pricingTip, _salesTargetLabel;
    protected NumberTextBox _salesTarget, _cost;

    protected MoneyLabel _fee;
    protected Button _doIt;

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);

    protected static final int DEFAULT_COIN_COST = 1000;
    protected static final int DEFAULT_SALES_TARGET = 50;

    protected static final Currency[] LISTABLE_CURRENCIES = { Currency.COINS, Currency.BARS };
}
