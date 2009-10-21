//
// $Id$

package client.shop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.DateUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.group.gwt.BrandDetail.BrandShare;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.item.gwt.CatalogListing.BasisItem;
import com.threerings.msoy.item.gwt.CatalogListing.DerivedItem;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.comment.CommentsPanel;
import client.item.BaseItemDetailPanel;
import client.item.DoListItemPopup;
import client.item.ItemUtil;
import client.item.ConfigButton;
import client.item.ShopUtil;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.PopupMenu;
import client.ui.ShareDialog;
import client.util.ClickCallback;
import client.util.Link;
import client.util.InfoCallback;

/**
 * Displays a detail view of an item from the catalog.
 */
public class ListingDetailPanel extends BaseItemDetailPanel
{
    public ListingDetailPanel (CatalogModels models, byte type, int catalogId)
    {
        addStyleName("listingDetailPanel");
        _models = models;

        // TODO: display loading swirly

        _catalogsvc.loadListing(type, catalogId, true, new InfoCallback<CatalogListing>() {
            public void onSuccess (CatalogListing result) {
                gotListing(result);
            }
        });
    }

    @Override
    protected void onLoad ()
    {
        super.onLoad();
        _singleton = this;
        configureBridge();
    }

    @Override
    protected void onUnload ()
    {
        _singleton = null;
        super.onUnload();
    }

    protected void gotListing (CatalogListing listing)
    {
        _listing = listing;
        init(listing.detail);

        if (listing.brand != null) {
            _creator.setBrand(listing.brand.group);
        }

        HorizontalPanel extras = new HorizontalPanel();
        extras.setStyleName("Extras");

        if (!CShell.isGuest() && isRemixable()) {
            extras.add(new ConfigButton(false, _msgs.listingRemix(),
                Link.createHandler(Pages.SHOP, ShopPage.REMIX, _item.getType(), _item.itemId,
                                    _listing.catalogId)));
        }
        extras.add(_configBtn = new ConfigButton(true, _msgs.listingConfig(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                ItemUtil.showViewerConfig();
            }
        }));
        _configBtn.setVisible(false);

        // create a table to display miscellaneous info and admin/owner actions
        SmartTable info = new SmartTable("Info", 0, 0);
        info.setText(0, 0, _msgs.listingListed(), 1, "What");
        info.setText(0, 1, DateUtil.formatDate(_listing.listedDate, false));
        info.setText(1, 0, _msgs.listingPurchases(), 1, "What");
        info.setText(1, 1, CatalogListing.PRICING_LIMITED_EDITION == _listing.pricing ?
                _msgs.limitedPurchases(""+_listing.purchases, ""+_listing.salesTarget) :
                ""+_listing.purchases);
        info.setText(2, 0, _msgs.favoritesCount(), 1, "What");
        info.setText(2, 1, "" + _listing.favoriteCount);
        extras.add(info);

        _indeets.add(extras);

        // if we are the creator (lister) of this item, or brand shareholder, allow us to delist it
        if (mayManage()) {
            HorizontalPanel controls = new HorizontalPanel();
            controls.setStyleName("controls");

            Label reprice = new Label(_msgs.listingReprice());
            reprice.addStyleName("actionLabel");
            reprice.addClickHandler(new ClickHandler() {
                public void onClick (ClickEvent event) {
                    DoListItemPopup.show(_item, _listing, new DoListItemPopup.ListedListener() {
                        public void itemListed (Item item, boolean updated) {
                            Link.replace(Pages.SHOP, "l", _item.getType(), _listing.catalogId);
                        }
                    });
                }
            });
            controls.add(reprice);

            Label delist = new Label(_msgs.listingDelist());
            String confirm = _msgs.listingDelistConfirm();
            if (_listing.derivationCount > 1) {
                confirm = _msgs.listingDelistBasisConfirmN("" + _listing.derivationCount);
            } else if (_listing.derivationCount > 0) {
                confirm = _msgs.listingDelistBasisConfirm1();
            }
            new ClickCallback<Void>(delist, confirm) {
                @Override protected boolean callService () {
                    _catalogsvc.removeListing(_item.getType(), _listing.catalogId, this);
                    return true;
                }
                @Override protected boolean gotResult (Void result) {
                    MsoyUI.info(_msgs.msgListingDelisted());
                    _models.itemDelisted(_listing);
                    History.back();
                    return false;
                }
            };
            controls.add(createSeparator());
            controls.add(delist);

            if (_listing.originalItemId != 0) {
                // also add a link to view the original
                controls.add(createSeparator());
                controls.add(Link.create(_msgs.listingViewOrig(), Pages.STUFF,
                                         "d", _item.getType(), _listing.originalItemId));
            }

            _details.add(controls);
        }

        // this will contain all of the buy-related interface and will be replaced with the
        // "bought" interface when the buying is done
        _details.add(new ItemBuyPanel(_listing, null));

        // display a comment interface below the listing details
        addTabBelow("Comments", new CommentsPanel(_item.getType(), listing.catalogId, true), true);

//         // if this item supports sub-items, add a tab for those item types
//         byte[] types = _item.getSalableSubTypes();
//         if (types.length > 0) {
//             for (int ii = 0; ii < types.length; ii++) {
//                 addTabBelow(_dmsgs.xlate("pItemType" + types[ii]), new Label("TBD"));
//             }
//         }
    }

    @Override
    protected void addExtraDetails ()
    {
        super.addExtraDetails();
        byte type = _listing.detail.item.getType();

        if (_listing.basis != null) {
            BasisItem basis = _listing.basis;
            FlowPanel basedOn = new FlowPanel();
            basedOn.add(new InlineLabel(_msgs.listingBasedOn() + " "));
            basedOn.add(_listing.basis.hidden ? new InlineLabel(basis.name) :
                Link.shopListingView(basis.name, type, basis.catalogId));
            basedOn.add(new InlineLabel(" " + _cmsgs.creatorBy() + " "));
            basedOn.add(basis.brand != null ?
                Link.groupView(basis.brand) : Link.memberView(basis.creator));
            _details.add(basedOn);
        }

        if (_listing.derivatives != null && _listing.derivatives.length > 0) {
            _usedBy = new FlowPanel();
            updateDerivatives();
            _details.add(_usedBy);
        }
    }

    @Override
    protected void addExtraThemeBits ()
    {
        if (DeploymentConfig.devDeployment && _item.getType() == Item.AVATAR && !CShell.isGuest()) {
            _itemsvc.loadLineups(_item.catalogId, new InfoCallback<GroupName[]>() {
                public void onSuccess (GroupName[] result) {
                    _lineup = new HashSet<GroupName>(Arrays.asList(result));
                    if (_managedThemes != null) {
                        buildLineup();
                    }
                }
            });
        }
    }

    @Override
    protected void gotManagedThemes (GroupName[] themes)
    {
        super.gotManagedThemes(themes);
        if (_lineup != null) {
            buildLineup();
        }
    }

    @Override // from BaseItemDetailPanel
    protected void addTagMenuItems (final String tag, PopupMenu menu)
    {
        menu.addMenuItem(_cmsgs.tagSearch(), new Command() {
            public void execute() {
                Link.go(Pages.SHOP, ShopUtil.composeArgs(_item.getType(), tag, null, 0));
            }
        });
    }

    @Override // from BaseItemDetailPanel
    protected void addRatableBits (HorizontalPanel row)
    {
        super.addRatableBits(row);

        // we need to create our share info lazily because _listing is null right now as we're
        // being called down to from our superclass constructor
        row.add(MsoyUI.makeShareButton(new ClickHandler() {
            public void onClick (ClickEvent event) {
                ShareDialog.Info info = new ShareDialog.Info();
                info.page = Pages.SHOP;
                info.args = Args.compose("l", _item.getType(), _listing.catalogId);
                info.what = _dmsgs.xlate("itemType" + _item.getType());
                info.title = _item.name;
                info.descrip = _item.description;
                info.image = _item.getThumbnailMedia();
                new ShareDialog(info).show();
           }
        }));
    }

    @Override
    protected boolean inShop ()
    {
        return true;
    }

    protected boolean mayManage ()
    {
        if (CShell.isSupport() || _detail.creator.getMemberId() == CShell.getMemberId()) {
            return true;
        }
        if (_listing.brand != null) {
            for (BrandShare shareHolder : _listing.brand.shareHolders) {
                if (shareHolder.member.getMemberId() == CShell.getMemberId()) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void updateDerivatives ()
    {
        while (_usedBy.getWidgetCount() > 0) {
            _usedBy.remove(0);
        }
        _usedBy.add(new InlineLabel(_msgs.listingUsedBy() + " "));
        final byte type = _listing.detail.item.getType();
        boolean first = true;
        for (DerivedItem derived : _listing.derivatives) {
            if (!first) {
                _usedBy.add(new InlineLabel(", "));
            }
            first = false;
            _usedBy.add(Link.shopListingView(derived.name, type, derived.catalogId));
        }
        if (_listing.derivatives.length < _listing.derivationCount) {
            final AsyncCallback<DerivedItem[]> showAll = new InfoCallback<DerivedItem[]>() {
                public void onSuccess (DerivedItem[] result) {
                    _listing.derivationCount = result.length;
                    _listing.derivatives = result;
                    updateDerivatives();
                }
            };
            ClickHandler action = new ClickHandler() {
                public void onClick (ClickEvent event) {
                    _catalogsvc.loadAllDerivedItems(type, _listing.catalogId, showAll);
                }
            };
            _usedBy.add(new InlineLabel(", "));
            _usedBy.add(MsoyUI.createActionLabel(
                _msgs.listingSeeAllDerivatives(), "inline", action));
        }
    }

    protected void buildLineup ()
    {
        // else we wait for that callback
        ensureThemeBits();
        _themeContents.setWidget(2, 0, _linePanel = new SmartTable());
        updateLineup();
    }

    protected void updateLineup ()
    {
        _linePanel.clear();
        if (_managedThemes.length == 0) {
            return;
        }

        _lineBox = new ListBox();
        _lineBox.addItem(_imsgs.itemListNoTheme());
        _lineBox.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                _lineButton.setEnabled(_lineBox.getSelectedIndex() > 0);
            }
        });
        _lineEntries = new ArrayList<GroupName>();

        _unlineBox = new ListBox();
        _unlineBox.addItem(_imsgs.itemListNoTheme());
        _unlineBox.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                _unlineButton.setEnabled(_unlineBox.getSelectedIndex() > 0);
            }
        });
        _unlineEntries = new ArrayList<GroupName>();

        for (GroupName theme : _managedThemes) {
            if (!_lineup.contains(theme)) {
                _lineBox.addItem(theme.toString());
                _lineEntries.add(theme);
            } else {
                _unlineBox.addItem(theme.toString());
                _unlineEntries.add(theme);
            }
        }
        int row = 0;
        if (_lineEntries.size() > 0) {
            _linePanel.setWidget(row, 0, _lineBox);
            _lineButton = MsoyUI.createTinyButton(_imsgs.itemLineupAdd(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    int ix = _lineBox.getSelectedIndex();
                    if (ix == 0) {
                        Popups.errorNear(_imsgs.itemNothingToStamp(), _lineButton);
                        return;
                    }
                    final GroupName theme = _lineEntries.get(ix-1);
                    _itemsvc.setAvatarInLineup(
                        _item.catalogId, theme.getGroupId(), true, new InfoCallback<Void>() {
                            public void onSuccess (Void result) {
                                CShell.log("Adding " + theme + " to _lineup...");
                                _lineup.add(theme);
                                CShell.log("Added " + theme + " to _lineup...");
                                updateLineup();
                            }
                        });
                }
            });
            _lineButton.setEnabled(false);
            _linePanel.setWidget(row, 1, _lineButton);
            row ++;
        }

        if (_unlineEntries.size() > 0) {
            _linePanel.setWidget(row, 0, _unlineBox);
            _unlineButton = MsoyUI.createTinyButton(_imsgs.itemLineupRemove(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    int ix = _unlineBox.getSelectedIndex();
                    if (ix == 0) {
                        Popups.errorNear(_imsgs.itemNothingToUnstamp(), _unlineButton);
                        return;
                    }
                    final GroupName theme = _unlineEntries.get(ix-1);
                    _itemsvc.setAvatarInLineup(
                        _item.catalogId, theme.getGroupId(), false, new InfoCallback<Void>() {
                            public void onSuccess (Void result) {
                                _lineup.remove(theme);
                                updateLineup();
                            }
                        });
                }
            });
            _unlineButton.setEnabled(false);
            _linePanel.setWidget(row, 1, _unlineButton);
            row ++;
        }
    }


    protected static Widget createSeparator ()
    {
        return new HTML("&nbsp;&nbsp;|&nbsp;&nbsp;");
    }

    /**
     * A callback from the studio viewer, to indicate that custom config is available.
     */
    protected static void hasCustomConfig ()
    {
        _singleton._configBtn.setVisible(true);
    }

    protected static native void configureBridge () /*-{
        $wnd.hasCustomConfig = function () {
            @client.shop.ListingDetailPanel::hasCustomConfig()();
        };
    }-*/;

    protected CatalogModels _models;
    protected CatalogListing _listing;

    protected ConfigButton _configBtn;
    protected FlowPanel _usedBy;

    protected Set<GroupName> _lineup;
    protected SmartTable _linePanel;
    protected ListBox _lineBox, _unlineBox;
    protected Button _lineButton, _unlineButton;
    protected List<GroupName> _lineEntries, _unlineEntries;

    protected static ListingDetailPanel _singleton;

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final CatalogServiceAsync _catalogsvc = GWT.create(CatalogService.class);
}
