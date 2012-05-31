//
// $Id$

package client.item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
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
import com.threerings.msoy.data.all.RatingHistoryResult;
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
import client.ui.Rating.HistoryService;
import client.ui.Rating.RateService;
import client.ui.Rating;
import client.ui.RoundBox;
import client.ui.StyledTabPanel;
import client.util.ClickCallback;
import client.util.FlashClients;
import client.util.InfoCallback;
import client.util.Link;
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

        // create the upper left: the item preview, ratings, favoring, etc
        _leftBits = new HeaderBox(null, _item.name);
        setWidget(0, 0, _leftBits);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HorizontalPanel.ALIGN_TOP);

        addLeftBits();

        // create the upper right: details, theme stuff and tags
        _rightBits = new FlowPanel();
        setWidget(0, 1, _rightBits);
        getFlexCellFormatter().setVerticalAlignment(0, 1, HorizontalPanel.ALIGN_TOP);

        SimplePanel detailHolder = new SimplePanel();
        detailHolder.setStyleName("Details");
        _rightBits.add(detailHolder);

        // a place for details
        _details = new RoundBox(RoundBox.BLUE);
        _details.setWidth("100%");
        detailHolder.setWidget(_details);

        addUpperDetails();
        addExtraDetails();
        addLowerDetails();

        _themeHolder = new SimplePanel();
        _themeHolder.setStyleName("Details");
        _rightBits.add(_themeHolder);

        _themeContents = new FlowPanel();
        if (!CShell.isGuest()) {
            _membersvc.loadManagedThemes(new InfoCallback<GroupName[]>() {
                public void onSuccess (GroupName[] result) {
                    _themeContents.add(_stampHeader = new SimplePanel());
                    _themeContents.add(_stampedBy = new FlowPanel());
                    _themeContents.add(_stampPanel = new SmartTable());

                    gotManagedThemes(result);
                    updateStamps();
                }
            });
        }
        addExtraThemeBits();

        // create the bottom: comments and such
        _bottomBits = new FlowPanel();
        setWidget(1, 0, _bottomBits, 2);
        getFlexCellFormatter().setVerticalAlignment(0, 1, HorizontalPanel.ALIGN_TOP);

        // add our tag business at the bottom
        addTagBits(detail);

        configureCallbacks(this);
    }

    protected void addTagBits (ItemDetail detail)
    {
        boolean canEditTags = CShell.isRegistered();
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

        Widget tagPanel = new TagDetailPanel(
            tagService, flagService, complainer, detail.tags, canEditTags);
        tagPanel.setHeight("10px");
        _rightBits.add(tagPanel);
    }

    protected void addLowerDetails ()
    {
        _details.add(WidgetUtil.makeShim(10, 10));
        _indeets = new RoundBox(RoundBox.WHITE);
        _indeets.addStyleName("Description");
        _details.add(_indeets);
        _indeets.add(MsoyUI.createRestrictedHTML(ItemUtil.getDescription(_item)));
    }

    protected void addUpperDetails ()
    {
        // set up our detail bits
        HorizontalPanel row = new HorizontalPanel();
        row.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        row.add(_creator = new CreatorLabel());
        _creator.setMember(_detail.creator);
        _details.add(row);

        // add a link to the creator's shop
        _creator.add(MsoyUI.createHTML("&nbsp;", "inline"));
        CatalogQuery query = new CatalogQuery();
        query.itemType = _detail.item.getType();
        query.creatorId = _detail.creator.name.getId();
        Widget bshop = Link.create(_imsgs.browseCatalogFor(), Pages.SHOP,
                                   ShopUtil.composeArgs(query, 0));
        bshop.setTitle(_imsgs.browseCatalogTip(_detail.creator.name.toString()));
        _creator.add(bshop);
    }

    protected void addLeftBits ()
    {
        SimplePanel preview = new SimplePanel();
        preview.setStyleName("ItemPreview");

        preview.setWidget(ItemUtil.createViewer(_item, inShop(), userOwnsItem(), _detail.memories));
        _leftBits.add(preview);
        if (_item.isRatable()) {
            HorizontalPanel row = new HorizontalPanel();
            row.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            addRatableBits(row);
            _leftBits.add(row);
        }
    }

    protected void gotManagedThemes (GroupName[] themes)
    {
        _managedThemes = themes;
    }

    protected void ensureThemeBits ()
    {
        if (_themeBits == null) {
            _themeBits = new RoundBox(RoundBox.BLUE);
            _themeBits.setWidth("100%");
            _themeBits.add(_themeContents);

            _themeHolder.setWidget(_themeBits);
        }
    }

    protected void addTabBelow (String title, Widget content, boolean select)
    {
        if (_belowTabs == null) {
            Widget widget = _belowTabs = new StyledTabPanel();
            _bottomBits.add(widget);
        }
        _belowTabs.add(content, title);
        if (select) {
            _belowTabs.selectTab(_belowTabs.getWidgetCount() - 1);
        }
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
        if (_managedThemes == null || _managedThemes.length == 0) {
            return;
        }

        if (_item.sourceId != 0) {
            _stampHeader.setWidget(MsoyUI.createLabel(
                "This item's listing:", "listingMarkupHeader"));
        } else {
            _stampHeader.setWidget(null);
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
        _stampEntries = Lists.newArrayList();

        _unstampBox = new ListBox();
        _unstampBox.addItem(_imsgs.itemListNoTheme());
        _unstampBox.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                _unstampButton.setEnabled(_unstampBox.getSelectedIndex() > 0);
            }
        });
        _unstampEntries = Lists.newArrayList();

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
            _stampButton = MsoyUI.createTinyButton(_imsgs.itemDoStamp(), null);
            new ClickCallback<Void>(_stampButton) {
                protected boolean callService () {
                    int ix = _stampBox.getSelectedIndex();
                    if (ix == 0) {
                        Popups.errorNear(_imsgs.itemNothingToStamp(), _stampButton);
                        return false;
                    }
                    _theme = _stampEntries.get(ix-1);
                    _itemsvc.stampItem(_item.getIdent(), _theme.getGroupId(), true, this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    _detail.themes.add(_theme);
                    updateStamps();
                    return true;
                }
                protected GroupName _theme;
            };
            _stampButton.setEnabled(false);
            _stampPanel.setWidget(row, 2, _stampButton);
            row ++;
        }

        if (_unstampBox.getItemCount() > 1) {
            _stampPanel.setWidget(row, 0, _unstampBox, 1);
            _unstampButton = MsoyUI.createTinyButton(_imsgs.itemDoUnstamp(), null);
            CShell.log("Created unstamp button: " + _unstampButton);
            new ClickCallback<Void>(_unstampButton) {
                protected boolean callService () {
                    int ix = _unstampBox.getSelectedIndex();
                    if (ix == 0) {
                        Popups.errorNear(_imsgs.itemNothingToUnstamp(), _unstampButton);
                        return false;
                    }
                    _theme = _unstampEntries.get(ix-1);
                    _itemsvc.stampItem(_item.getIdent(), _theme.getGroupId(), false, this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    _detail.themes.remove(_theme);
                    updateStamps();
                    return true;
                }
                protected GroupName _theme;
            };
            CShell.log("Disabling unstamp button...");
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
        row.add(new Rating(_item.getRating(), _item.ratingCount,
            _detail.memberItemInfo.memberRating, true, new RateService() {
            public void handleRate (byte newRating, InfoCallback<RatingResult> callback) {
                _itemsvc.rateItem(_item.getIdent(), newRating, callback);
            }
        }, new HistoryService() {
            public void getHistory (InfoCallback<RatingHistoryResult> callback) {
                _itemsvc.getRatingHistory(_item.getIdent(), callback);
            }
        }));

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

    protected FlowPanel _leftBits, _rightBits, _bottomBits;

    protected RoundBox _details;
    protected RoundBox _indeets;
    protected RoundBox _themeBits;

    protected FlowPanel _themeContents;
    protected SimplePanel _themeHolder;

    protected CreatorLabel _creator;

    protected FlowPanel _stampedBy;
    protected SimplePanel _stampHeader;
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

    protected static final int BRIEF_STAMP_COUNT = 3;
}
