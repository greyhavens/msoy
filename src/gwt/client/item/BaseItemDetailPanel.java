//
// $Id$

package client.item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.TagHistory;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.shell.CShell;
import client.ui.CreatorLabel;
import client.ui.HeaderBox;
import client.ui.MsoyUI;
import client.ui.PopupMenu;
import client.ui.Rating;
import client.ui.RoundBox;
import client.ui.StyledTabPanel;
import client.util.FlashClients;
import client.util.Link;
import client.util.InfoCallback;
import client.util.NoopAsyncCallback;

/**
 * Defines the base item detail popup from which we derive an inventory and catalog item detail.
 */
public abstract class BaseItemDetailPanel extends SmartTable
{
    protected BaseItemDetailPanel ()
    {
        super("itemDetailPanel", 0, 10);
    }

    protected void init (ItemDetail detail)
    {
        _detail = detail;
        _item = detail.item;

        HeaderBox bits = new HeaderBox(null, _item.name);
        SimplePanel preview = new SimplePanel();
        preview.setStyleName("ItemPreview");

        preview.setWidget(ItemUtil.createViewer(_item, inShop(), userOwnsItem(), detail.memories));
        bits.add(preview);
        if (_item.isRatable()) {
            HorizontalPanel row = new HorizontalPanel();
            row.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            addRatableBits(row);
            bits.add(row);
        }
        setWidget(0, 0, bits);
        getFlexCellFormatter().setRowSpan(0, 0, 3);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HorizontalPanel.ALIGN_TOP);

        // a place for details
        setWidget(0, 1, _details = new RoundBox(RoundBox.BLUE), 1, "Details");
        _details.setWidth("100%");
        getFlexCellFormatter().setVerticalAlignment(0, 1, HorizontalPanel.ALIGN_TOP);

        // set up our detail bits
        _details.add(_creator = new CreatorLabel());
        _creator.setMember(_detail.creator);

        // add a link to the creator's shop
        _creator.add(MsoyUI.createHTML("&nbsp;", "inline"));
        CatalogQuery query = new CatalogQuery();
        query.itemType = _detail.item.getType();
        query.creatorId = _detail.creator.getMemberId();
        Widget bshop = Link.create(_imsgs.browseCatalogFor(), Pages.SHOP,
                                   ShopUtil.composeArgs(query, 0));
        bshop.setTitle(_imsgs.browseCatalogTip(_detail.creator.toString()));
        _creator.add(bshop);

        addExtraDetails();

        _details.add(WidgetUtil.makeShim(10, 10));
        _indeets = new RoundBox(RoundBox.WHITE);
        _indeets.addStyleName("Description");
        _details.add(_indeets);
        _indeets.add(MsoyUI.createRestrictedHTML(ItemUtil.getDescription(_item)));

        _themeContents = new SmartTable();

        if (!CShell.isGuest()) {
            _membersvc.loadManagedThemes(new InfoCallback<GroupName[]>() {
                public void onSuccess (GroupName[] result) {
                    _themeContents.setWidget(0, 0, _stampedBy = new FlowPanel());
                    _themeContents.setWidget(1, 0, _stampPanel = new SmartTable());

                    gotManagedThemes(result);
                    updateStamps();
                }
            });
        }
        addExtraThemeBits();

        // add our tag business at the bottom
        boolean canEditTags = CShell.isSubscriber() || _item.creatorId == CShell.getMemberId();
        TagDetailPanel.TagService tagService = new TagDetailPanel.TagService() {
            public void tag (String tag, AsyncCallback<TagHistory> callback) {
                _itemsvc.tagItem(_item.getIdent(), tag, true, callback);
            }
            public void untag (String tag, AsyncCallback<TagHistory> callback) {
                _itemsvc.tagItem(_item.getIdent(), tag, false, callback);
            }
            public void getTagHistory (AsyncCallback<List<TagHistory>> callback) {
                _itemsvc.getTagHistory(_item.getIdent(), callback);
            }
            public void getTags (AsyncCallback<List<String>> callback) {
                _itemsvc.getTags(_item.getIdent(), callback);
            }
            public void addMenuItems (String tag, PopupMenu menu, boolean canEdit) {
                addTagMenuItems(tag, menu);
            }
        };
        TagDetailPanel.FlagService flagService = new TagDetailPanel.FlagService() {
            public void addFlag (final ItemFlag.Kind kind, String comment) {
                ItemIdent ident = new ItemIdent(_item.getType(), _item.getMasterId());
                _itemsvc.addFlag(ident, kind, comment, new NoopAsyncCallback());
            }
        };
        TagDetailPanel.ComplainService complainer = new TagDetailPanel.ComplainService() {
            public void complain (String tag, String reason, AsyncCallback<Void> callback) {
                ItemIdent ident = new ItemIdent(_item.getType(), _item.getMasterId());
                _itemsvc.complainTag(ident, tag, reason, callback);
            }
        };
        setWidget(2, 0, new TagDetailPanel(
            tagService, flagService, complainer, detail.tags, canEditTags));
        getFlexCellFormatter().setHeight(2, 0, "10px");

        configureCallbacks(this);
    }

    protected void gotManagedThemes (GroupName[] themes)
    {
        _managedThemes = themes;
    }

    protected void ensureThemeBits ()
    {
        if (_themeBits == null) {
            setWidget(1, 0, _themeBits = new RoundBox(RoundBox.BLUE), 1, "Details");
            getFlexCellFormatter().setVerticalAlignment(1, 0, HorizontalPanel.ALIGN_TOP);
            _themeBits.setWidth("100%");
            _themeBits.add(_themeContents);
        }
    }

    protected void addTabBelow (String title, Widget content, boolean select)
    {
        if (_belowTabs == null) {
            addBelow(_belowTabs = new StyledTabPanel());
        }
        _belowTabs.add(content, title);
        if (select) {
            _belowTabs.selectTab(_belowTabs.getWidgetCount() - 1);
        }
    }

    /**
     * Adds a widget below the primary item detail contents.
     */
    protected void addBelow (Widget widget)
    {
        int row = getRowCount();
        setWidget(row, 0, widget);
        getFlexCellFormatter().setColSpan(row, 0, 3);
    }

    protected void updateStamps ()
    {
        updateStampedBy();
        updateStampItem();
    }

    protected void updateStampedBy ()
    {
        while (_stampedBy.getWidgetCount() > 0) {
            _stampedBy.remove(0);
        }
        int cnt = _detail.themes.size();
        if (cnt == 0) {
            return;
        }

        // now that we know we're drawing something, make sure the UI is showing the theme box
        ensureThemeBits();

        if (_briefStamps) {
            cnt = Math.min(cnt, BRIEF_STAMP_COUNT);
        }
        _stampedBy.add(new InlineLabel(_imsgs.itemStampedBy() + " "));
        for (int ii = 0; ii < cnt; ii ++) {
            GroupName theme = _detail.themes.get(ii);
            if (ii > 0) {
                _stampedBy.add(new InlineLabel(", "));
            }
            _stampedBy.add(Link.groupView(theme));
        }
        if (_briefStamps && _detail.themes.size() > BRIEF_STAMP_COUNT) {
            ClickHandler action = new ClickHandler() {
                public void onClick (ClickEvent event) {
                    _briefStamps = false;
                    updateStamps();
                }
            };
            _stampedBy.add(new InlineLabel(", "));
            _stampedBy.add(MsoyUI.createActionLabel(_imsgs.itemSeeAllThemes(), "inline", action));
        }
    }

    protected void updateStampItem ()
    {
        _stampPanel.clear();
        if (_managedThemes == null || _managedThemes.length == 0 || _item.isCatalogClone()) {
            return;
        }

        // now that we know we're drawing something, make sure the UI is showing the theme box
        ensureThemeBits();

        _stampBox = new ListBox();
        _stampBox.addItem(_imsgs.itemListNoTheme());
        _stampBox.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                _stampButton.setEnabled(_stampBox.getSelectedIndex() > 0);
            }
        });
        _stampEntries = new ArrayList<GroupName>();

        _unstampBox = new ListBox();
        _unstampBox.addItem(_imsgs.itemListNoTheme());
        _unstampBox.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                _unstampButton.setEnabled(_unstampBox.getSelectedIndex() > 0);
            }
        });
        _unstampEntries = new ArrayList<GroupName>();

        Set<GroupName> existing = new HashSet<GroupName>(_detail.themes);
        for (GroupName theme : _managedThemes) {
            if (existing.contains(theme)) {
                if (!restrictUnstamping(theme)) {
                    _unstampBox.addItem(theme.toString());
                    _unstampEntries.add(theme);
                }
            } else {
                _stampBox.addItem(theme.toString());
                _stampEntries.add(theme);
            }
        }

        int row = 0;
        if (_stampBox.getItemCount() > 1) {
            _stampPanel.setWidget(row, 0, _stampBox, 1);
            _stampButton = MsoyUI.createTinyButton(_imsgs.itemDoStamp(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    int ix = _stampBox.getSelectedIndex();
                    if (ix == 0) {
                        Popups.errorNear(_imsgs.itemNothingToStamp(), _stampButton);
                        return;
                    }
                    final GroupName theme = _stampEntries.get(ix-1);
                    _itemsvc.stampItem(
                        _item.getIdent(), theme.getGroupId(), true, new InfoCallback<Void>() {
                            public void onSuccess (Void result) {
                                _detail.themes.add(theme);
                                updateStamps();
                            }
                        });
                }
            });
            _stampButton.setEnabled(false);
            _stampPanel.setWidget(row, 2, _stampButton);
            row ++;
        }

        if (_unstampBox.getItemCount() > 1) {
            _stampPanel.setWidget(row, 0, _unstampBox, 1);
            _unstampButton = MsoyUI.createTinyButton(_imsgs.itemDoUnstamp(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    int ix = _unstampBox.getSelectedIndex();
                    if (ix == 0) {
                        Popups.errorNear(_imsgs.itemNothingToUnstamp(), _unstampButton);
                        return;
                    }
                    final GroupName theme = _unstampEntries.get(ix-1);
                    _itemsvc.stampItem(
                        _item.getIdent(), theme.getGroupId(), false, new InfoCallback<Void>() {
                            public void onSuccess (Void result) {
                                _detail.themes.remove(theme);
                                updateStamps();
                            }
                        });
                }
            });
            _unstampButton.setEnabled(false);
            _stampPanel.setWidget(row, 2, _unstampButton);
            row ++;
        }
    }

    /**
     * Overridden by subclasses that want to restrict the unstamping of some themes. This is
     * currently used by {@link ListingDetailPanel} to prevent unstamping avatar lineup items.
     */
    protected boolean restrictUnstamping (GroupName theme)
    {
        return false;
    }

    /**
     * Add any menu items to the tag widget.
     */
    protected void addTagMenuItems (String tag, PopupMenu menu)
    {
        // nothing here
    }

    /**
     * Adds extra items to the _details widget, just after the creator profile and browse links.
     */
    protected void addExtraDetails ()
    {
        // nothing in base class
    }

    /**
     * Adds extra items to the _themeBits widget.
     */
    protected void addExtraThemeBits ()
    {
        // nothing in base class
    }

    protected void addRatableBits (HorizontalPanel row)
    {
        Rating rating = new Rating(
            _item.getRating(), _item.ratingCount, _detail.memberItemInfo.memberRating, true) {
            @Override protected void handleRate (
                byte newRating , InfoCallback<RatingResult> callback) {
                _itemsvc.rateItem(_item.getIdent(), newRating, callback);
            }
        };
        row.add(rating);

        if (!CShell.isGuest()) {
            row.add(new FavoriteIndicator(_item, _detail.memberItemInfo));
        }
    }

    /**
     * Are we showing in the shop?
     */
    protected boolean inShop ()
    {
        return false;
    }

    /**
     * Returns true if the user owns this specific item.
     */
    protected boolean userOwnsItem ()
    {
        return false; // overrideable in subclasses
    }

    /**
     * Returns true if the item we're displaying is remixable.
     */
    protected boolean isRemixable ()
    {
        return (_item != null) && _item.getPrimaryMedia().isRemixable();
    }

    /**
     * Called from the avatarviewer to effect a scale change.
     */
    protected void updateAvatarScale (float newScale)
    {
        if (!(_item instanceof Avatar)) {
            return;
        }

        Avatar av = (Avatar) _item;
        if (av.scale != newScale) {
            // stash the new scale in the item
            av.scale = newScale;
            _scaleUpdated = true;

            // try immediately updating in the whirled client
            Element client = FlashClients.findClient();
            if (client != null) {
                sendAvatarScaleToWorld(client, av.itemId, newScale);
            }
        }
    }

    /**
     * Called when the user clicks our up arrow.
     */
    protected void onUpClicked ()
    {
        History.back();
    }

    @Override // Panel
    protected void onDetach ()
    {
        super.onDetach();

        // persist our new scale to the server
        if (_scaleUpdated && _item.ownerId == CShell.getMemberId()) {
            _itemsvc.scaleAvatar(
                _item.itemId, ((Avatar) _item).scale, new InfoCallback.NOOP<Void>());
        }

        // tell the client we're not playing music anymore
        updateMediaPlayback(false);
    }

    /**
     * Tells the client whether music is playing.
     */
    protected void updateMediaPlayback (boolean started)
    {
        Element client = FlashClients.findClient();
        if (client != null) {
            updateMediaPlaybackNative(client, started);
        }
    }

    /**
     * Configures interface to be called by the avatarviewer.
     */
    protected static native void configureCallbacks (BaseItemDetailPanel panel) /*-{
        $wnd.updateAvatarScale = function (newScale) {
            panel.@client.item.BaseItemDetailPanel::updateAvatarScale(F)(newScale);
        };
        $wnd.gwtMediaPlayback= function (started) {
            panel.@client.item.BaseItemDetailPanel::updateMediaPlayback(Z)(started);
        };
    }-*/;

    protected static native void sendAvatarScaleToWorld (
        Element client, int avatarId, float newScale) /*-{
        client.updateAvatarScale(avatarId, newScale);
    }-*/;

    protected static native void updateMediaPlaybackNative (Element client, boolean started) /*-{
        client.gwtMediaPlayback(started);
    }-*/;

    protected Item _item;
    protected ItemDetail _detail;
    protected GroupName[] _managedThemes;

    protected RoundBox _details;
    protected RoundBox _indeets;
    protected RoundBox _themeBits;
    protected SmartTable _themeContents;

    protected CreatorLabel _creator;

    protected FlowPanel _stampedBy;
    protected boolean _briefStamps = true;

    protected SmartTable _stampPanel;
    protected ListBox _stampBox, _unstampBox;
    protected Button _stampButton, _unstampButton;
    protected List<GroupName> _stampEntries, _unstampEntries;

    protected StyledTabPanel _belowTabs;

    /** Have we updated the scale (of an avatar?) */
    protected boolean _scaleUpdated;

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
    protected static final ItemServiceAsync _itemsvc = GWT.create(ItemService.class);
    protected static final WebMemberServiceAsync _membersvc = GWT.create(WebMemberService.class);

    protected static final int BRIEF_STAMP_COUNT = 6;
}
