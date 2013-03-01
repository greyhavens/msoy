//
// $Id$

package client.item;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.group.gwt.BrandDetail;
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

        String title = item.isListedOriginal() ? _imsgs.doListUpdateTitle()
                                                : _imsgs.doListCreateTitle();
        CShell.frame.showDialog(title, new DoListItemPopup(item, listing, listener));
    }

    protected DoListItemPopup (Item item, final CatalogListing listing, ListedListener listener)
    {
        addStyleName("doListItem");

        _item = item;
        _listener = listener;

        // note whether we are listing this item for the first time or updating its listing or
        // whether or not we're repricing an existing listing
        // TODO RELISTING
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
            rating.addWidget(MsoyUI.createHTML(_imsgs.doListRatingIntro(), null), 2);
            rating.addWidget(WidgetUtil.makeShim(5, 5), 2);
            int row = rating.addText(_imsgs.doListRating(), 1);
            rating.getFlexCellFormatter().setWidth(row, 0, "150px"); // yay html!
            rating.setWidget(row, 1, _stars, 1);
            row = rating.addText(_imsgs.doListFee(), 1);
            _fee = new MoneyLabel(Currency.COINS, getMinimumPrice(Currency.COINS));
            rating.setWidget(row, 1, _fee, 1);
            add(MsoyUI.createLabel(_imsgs.doListRatingHeader(), "Header"));
            add(rating);

        } else if (firstTime) {
            // it's not a salable item, so the rating doesn't matter, but we need something
            _stars.setRating(5);

        } else if (repricing) {
            _stars.setRating(listing.detail.item.getRating());
        }

        // possibly add the brand selection ui
        if (_item.getType().isBrandableType() && (firstTime || repricing)) {
            SmartTable brand = new SmartTable(0, 3);
            brand.addWidget(MsoyUI.createHTML(_imsgs.doListSelectBrandIntro(), null), 3);
            brand.addWidget(WidgetUtil.makeShim(5, 5), 3);

            int row = brand.addText(_imsgs.doListBrand(), 1, "rightLabel");
            brand.setWidget(row, 1, _brandBox = new ListBox(), 1);
            _brandBox.addItem(_imsgs.doListNoBrand());

            _catalogsvc.loadManagedBrands(new InfoCallback<List<BrandDetail>>() {
                @Override public void onSuccess (List<BrandDetail> result) {
                    gotBrands(result, listing);
                }
            });

            add(MsoyUI.createLabel(_imsgs.doListSelectBrandHeader(), "Header"));
            add(brand);
        }

        // possibly add the basis selection ui
        if (_item.getType().isDerivationType() && (firstTime || repricing)) {
            SmartTable basis = new SmartTable(0, 3);
            basis.addWidget(MsoyUI.createHTML(_imsgs.doListSelectBasisIntro(), null), 3);
            basis.addWidget(WidgetUtil.makeShim(5, 5), 3);

            int row = basis.addText(_imsgs.doListBasis(), 1, "rightLabel");
            basis.setWidget(row, 1, _basisBox = new ListBox(), 1);
            _basisBox.addItem(_imsgs.doListNoBasis());
            _basisBox.addChangeHandler(new ChangeHandler() {
                public void onChange (ChangeEvent event) {
                    ListingCard basis = getBasis();
                    if (basis != null) {
                        setCurrency(basis.currency);
                    }
                }
            });

            _catalogsvc.loadPotentialBasisItems(_item.getType(),
                new InfoCallback<List<ListingCard>>() {
                    @Override public void onSuccess (List<ListingCard> result) {
                        gotBasisItems(result, listing);
                    }
            });

            add(MsoyUI.createLabel(_imsgs.doListSelectBasisHeader(), "Header"));
            add(basis);
        }

        // possibly add the pricing selection UI
        if (_item.isSalable() && (firstTime || repricing)) {
            SmartTable pricing = new SmartTable(0, 3);

            int row = pricing.addWidget(
                MsoyUI.createHTML(_imsgs.doListPricingIntro(), null), 3);
            pricing.addWidget(WidgetUtil.makeShim(5, 5), 3);

            row = pricing.addText(_imsgs.doListStrategy(), 1, "rightLabel");
            pricing.setWidget(row, 1, _pricingBox = new ListBox(), 1);
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
            pricing.setWidget(row, 1, _salesTarget = new NumberTextBox(false, 5, 5), 1);
            int salesTarget = (listing == null) ? DEFAULT_SALES_TARGET : listing.salesTarget;
            _salesTarget.setNumber(salesTarget);

            pricing.addWidget(WidgetUtil.makeShim(5, 5), 3);

            if (listing != null) {
                setCurrency(listing.quote.getListedCurrency());
                if (listing.basisId > 0) {
                    _catalogsvc.loadListing(item.getType(), listing.basisId, false,
                        new InfoCallback<CatalogListing>() {
                            public void onSuccess (CatalogListing result) {
                                _previousBasis = result;
                            }
                        });
                }
            }

            row = pricing.addText(_imsgs.doListCost(), 1, "rightLabel");
            pricing.setWidget(row, 1, _cost = new NumberTextBox(false, 7, 7), 1);
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
                    _catalogsvc.listItem(_item.getIdent(), rating, getPricing(), getSalesTarget(),
                        getCurrency(), getCost(), getBasisId(), getBrandId(), this);
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
            new ClickCallback<Void>(_doIt) {
                @Override protected void updateConfirmMessage () {
                    int dcount = listing.derivationCount;
                    if (dcount == 0) {
                        setConfirmText(null);
                        return;
                    }
                    int costChange = getCost() - listing.quote.getListedAmount();
                    if (getCurrency() != listing.quote.getListedCurrency()) {
                        setConfirmText(dcount > 1 ?
                            _imsgs.doListChangeBasisCurrencyConfirmN(""+dcount) :
                            _imsgs.doListChangeBasisCurrencyConfirm1());
                    } else if (costChange != 0) {
                        String did = costChange > 0 ? _imsgs.doListUp() : _imsgs.doListDown();
                        String cstr = ""+Math.abs(costChange);
                        setConfirmText(dcount > 1 ?
                            _imsgs.doListRepriceBasisConfirmN(did, cstr, ""+dcount) :
                            _imsgs.doListRepriceBasisConfirm1(did, cstr));
                    }
                }
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
                                              salesTarget, getCurrency(), getCost(), getBasisId(),
                                              getBrandId(), this);
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
                    _listener.itemListed(_item, true);
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
        return Currency.COINS;
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

    protected int getBasisId ()
    {
        ListingCard basis = getBasis();
        return basis == null ? 0 : basis.catalogId;
    }

    protected int getBrandId ()
    {
        if (_brandBox == null) {
            return 0;
        }
        int ix = _brandBox.getSelectedIndex();
        if (ix == 0) {
            return 0;
        }
        return _brandItems.get(ix - 1).group.getGroupId();
    }

    protected void setCurrency (Currency currency)
    {
        // Nothing at all
    }

    protected void gotBasisItems (List<ListingCard> items, CatalogListing listing)
    {
        for (ListingCard item : items) {
            _basisBox.addItem(item.name);
            _basisItems.add(item);
            if (listing != null && item.catalogId == listing.basisId) {
                _basisBox.setSelectedIndex(_basisBox.getItemCount() - 1);
            }
        }
    }

    protected void gotBrands (List<BrandDetail> result, CatalogListing listing)
    {
        // if the listing is branded by a brand we do not administer (this could happen if
        // we clicked Reprice as support+), include the item's existing brand in the dropdown
        if (listing != null && listing.brand != null && !result.contains(listing.brand)) {
            result.add(listing.brand);
        }
        for (BrandDetail detail : result) {
            _brandBox.addItem(detail.group.toString());
            _brandItems.add(detail);
            if (listing != null && listing.brand != null &&
                    detail.group.equals(listing.brand.group)) {
                _brandBox.setSelectedIndex(_brandBox.getItemCount() - 1);
            }
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
    protected List<ListingCard> _basisItems = Lists.newArrayList();
    protected CatalogListing _previousBasis;

    protected ListBox _brandBox;
    protected List<BrandDetail> _brandItems = Lists.newArrayList();

    protected ListBox _pricingBox;
    protected Label _pricingTip, _salesTargetLabel;
    protected NumberTextBox _salesTarget, _cost;

    protected MoneyLabel _fee;
    protected Button _doIt;

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final CatalogServiceAsync _catalogsvc = GWT.create(CatalogService.class);

    protected static final int DEFAULT_COIN_COST = 1000;
    protected static final int DEFAULT_SALES_TARGET = 50;

    protected static final Currency[] LISTABLE_CURRENCIES = { Currency.COINS, Currency.BARS };
}
