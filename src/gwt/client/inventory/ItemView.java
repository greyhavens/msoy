//
// $Id$

package client.inventory;

import java.util.Collection;
import java.util.Iterator;

import client.item.ItemRating;
import client.shell.MsoyEntryPoint;
import client.util.HeaderValueTable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.web.Audio;
import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.Document;
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemDetail;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.Pet;
import com.threerings.msoy.item.web.Photo;
import com.threerings.msoy.item.web.TagHistory;
import com.threerings.msoy.web.client.WebContext;

public class ItemView extends PopupPanel
{
    public ItemView (WebContext ctx, Item item, ItemPanel parent)
    {
        super(true);
        _item = item;
        _ctx = ctx;
        _itemId = new ItemIdent(_item.getType(), _item.getProgenitorId());
        _parent = parent;
        setStyleName("itemPopup");
        setWidget(_content = new DockPanel());

        _table = new HeaderValueTable();
        _content.add(_table, DockPanel.CENTER);

        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("itemDetailErrors");
        _content.add(_errorContainer, DockPanel.NORTH);

        _ctx.itemsvc.loadItemDetail(_ctx.creds, _itemId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _itemDetail = (com.threerings.msoy.item.web.ItemDetail) result;
                buildUI();
            }
            public void onFailure (Throwable caught) {
                GWT.log("loadInventory failed", caught);
                // TODO: if ServiceException, translate
                addError("Failed to load item detail: " + caught);
            }
        });
    }

    protected void buildUI () {
        if (_item.parentId != -1) {
            _table.addHeader("Clone Information");
            _table.addRow("Clone ID", String.valueOf(_item.itemId),
                   "Owner", String.valueOf(_item.ownerId));
        }
        _table.addHeader("Generic Item Information");
        // TODO: flags should be checkboxes when we have some?
        _table.addRow("Item ID", String.valueOf(_item.getProgenitorId()),
               "Flag mask", String.valueOf(_item.flags));
        String owner;
        if (_item.ownerId == _ctx.creds.memberId) {
            owner = "You";
        } else if (_itemDetail.owner != null) {
            owner = _itemDetail.owner.toString();
        } else if (_item.parentId == -1) {
            owner = "<catalog>";
        } else {
            // this only happens if we're looking at another member's clone
            // which is probably an admin function?
            owner = "Member #" + _item.ownerId;
        }
        _table.addRow("Owner", owner, "Creator", _itemDetail.creator.toString());

        Widget thumbWidget = _item.thumbMedia == null ?
            new Label("(default)") : ItemContainer.createContainer(_item.getThumbnailMedia(), true);
        Widget furniWidget = _item.furniMedia == null ?
            new Label("(default)") : ItemContainer.createContainer(_item.getFurniMedia(), false);
        _table.addRow("Thumbnail", thumbWidget, "Furniture", furniWidget);

        // TODO: Maybe merge ItemDetail and ItemEditor, so we could put these
        // TODO: subclass-specific bits into (and rename) e.g. DocumentEditor?
        if (_item instanceof Document) {
            _table.addHeader("Document Information");
            _table.addRow("Title", ((Document)_item).title);
            // we should check if the document has a useful visual
            String url = MsoyEntryPoint.toMediaPath(((Document)_item).docMedia.getMediaPath());
            _table.addRow("Document Media", new HTML("<A HREF='" + url + "'>" + url + "</a>"));

        } else if (_item instanceof Furniture) {
            _table.addHeader("Furniture Information");
            _table.addRow("Action", ((Furniture)_item).action,
                "Description", ((Furniture)_item).description);

        } else if (_item instanceof Pet) {
            _table.addHeader("Pet Information");
            _table.addRow("Description", ((Pet)_item).description);

        } else if (_item instanceof Game) {
            _table.addHeader("Game Information");
            _table.addRow("Name", ((Game)_item).name,
                   "# Players (Desired)", String.valueOf(((Game)_item).desiredPlayers));
            _table.addRow("# Players (Minimum)", String.valueOf(((Game)_item).minPlayers),
                   "# Players (Maximum)", String.valueOf(((Game)_item).maxPlayers));
            int gameId = _item.getProgenitorId();
            String href = "<a href=\"game.html#" + gameId + "\">";
            _table.addRow("Play", new HTML(href + "Play now</a>"));

        } else if (_item instanceof Photo) {
            _table.addHeader("Photo Information");
            MediaDesc photoMedia = ((Photo)_item).photoMedia;
            Widget photoContainer;
            photoContainer = ItemContainer.createContainer(photoMedia, false);
            _table.addRow("Photo", photoContainer);
            _table.addRow("Caption", ((Photo)_item).caption);

        } else if (_item instanceof Avatar) {
            _table.addHeader("Avatar Information");
            MediaDesc avatarMedia = ((Avatar)_item).avatarMedia;
            String path = MsoyEntryPoint.toMediaPath(
                avatarMedia.getMediaPath());
            Widget avatarViewer = WidgetUtil.createFlashContainer(
                "avatarViewer", "/clients/avatarviewer.swf", 300, 500,
                "avatar=" + URL.encodeComponent(path));
            _table.addRow("Description", ((Avatar)_item).description);
            _table.addRow("Avatar", avatarViewer);

        } else if (_item instanceof Audio) {
            _table.addHeader("Audio Information");
            MediaDesc audioMedia = ((Audio)_item).audioMedia;
            Widget audioContainer;
            audioContainer = ItemContainer.createContainer(audioMedia, false);
            _table.addRow("Description", ((Audio)_item).description);
            _table.addRow("Audio", audioContainer);

        } else {
            _table.addHeader("UNKNOWN OBJECT TYPE: " + _item.getType());
        }

        _table.addHeader("Rating Information");

        _ratingImage = new ItemRating(_ctx, _itemDetail.item, _itemDetail.memberRating);
        _table.addRow("Rating", _ratingImage);

        _table.addHeader("Tagging Information");

        TextBox newTagBox = new TextBox();
        newTagBox.setMaxLength(20);
        newTagBox.setVisibleLength(12);
        newTagBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                clearErrors();
                String tagName = ((TextBox) sender).getText().toLowerCase();
                if (tagName.length() > 24) {
                    addError("Invalid tag: can't be more than 24 characters.");
                    return;
                }
                for (int i = 0; i < tagName.length(); i ++) {
                    char c = tagName.charAt(i);
                    if (Character.isLetter(c) || !Character.isDigit(c) || c == '_') {
                        continue;
                    }
                    addError(
                        "Invalid tag: use letters, numbers, and underscore.");
                    return;
                }
                _ctx.itemsvc.tagItem(_ctx.creds, _itemId, tagName, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        updateTags();
                    }
                    public void onFailure (Throwable caught) {
                        GWT.log("tagItem failed", caught);
                        addError("Internal error adding tag: " + caught.getMessage());
                    }
                });
                ((TextBox) sender).setText(null);
            }
        });

        _historicalTags = new ListBox();
        _historicalTags.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                clearErrors();
                ListBox box = (ListBox) sender;
                String value = box.getValue(box.getSelectedIndex());
                _ctx.itemsvc.tagItem(_ctx.creds, _itemId, value, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        updateTags();
                    }
                    public void onFailure (Throwable caught) {
                        GWT.log("tagItem failed", caught);
                        addError("Internal error adding tag: " + caught.getMessage());
                    }
                });
            }
        });

        _tagContainer = new FlowPanel();
        // it seems only an explicit width will cause the label to wrap
        _tagContainer.setWidth("400px");
        ComplexPanel enterTagContainer = new HorizontalPanel();
        enterTagContainer.add(newTagBox);
        enterTagContainer.add(new HTML(" &nbsp; "));
        enterTagContainer.add(_historicalTags);
        _table.addRow("Enter a new tag", enterTagContainer);
        _table.addRow("Tags", _tagContainer);
        updateTags();

        Button button = new Button("Hide/Show");
        button.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                toggleTagHistory();
            }
        });
        _table.addRow("Tagging History", button);

        _table.addHeader("Item Actions");

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(5);
        _table.addRow(buttons);

        if (_item.parentId == -1) {
            button = new Button("List in Catalog ...");
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    listItem(_item.getIdent());
                }
            });

        } else {
            button = new Button("Remix ...");
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    remixItem(_item.getIdent());
                }
            });
        }
        buttons.add(button);

        if (_item.parentId == -1) {
            button = new Button("Edit ...");
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    ItemEditor editor = _parent.createItemEditor(_item.getType());
                    editor.setItem(_item);
                    editor.show();
                }
            });
            buttons.add(button);
        }

        // add a status label
        _table.addRow(_status = new Label(""));

        recenter(false);
    }

    /**
     * Recenters our popup.
     */
    protected void recenter (boolean defer)
    {
        if (defer) {
            DeferredCommand.add(new Command() {
                public void execute () {
                    recenter(false);
                }
            });
        } else {
            setPopupPosition((Window.getClientWidth() - getOffsetWidth()) / 2,
                             (Window.getClientHeight() - getOffsetHeight()) / 2);
        }
    }

    protected void updateTags ()
    {
        _ctx.itemsvc.getTagHistory(_ctx.creds, _ctx.creds.memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _historicalTags.clear();
                Iterator i = ((Collection) result).iterator();
                while (i.hasNext()) {
                    TagHistory history = (TagHistory) i.next();
                    if (history.member.getMemberId() == _ctx.creds.memberId) {
                        if (history.tag != null) {
                            _historicalTags.addItem(history.tag);
                        }
                    }
                }
                _historicalTags.setVisible(_historicalTags.getItemCount() > 0);
            }
            public void onFailure (Throwable caught) {
                GWT.log("getTagHistory failed", caught);
                addError("Internal error fetching tag history: " + caught.getMessage());
            }
        });

        _ctx.itemsvc.getTags(_ctx.creds, _itemId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _tagContainer.clear();
                boolean first = true;
                Iterator i = ((Collection) result).iterator();
                StringBuffer builder = new StringBuffer();
                while (i.hasNext()) {
                    String tag = (String) i.next();
                    if (!first) {
                        builder.append(" . ");
                    }
                    first = false;
                    builder.append(tag);
                }
                _tagContainer.add(new Label(builder.toString(), true));
            }
            public void onFailure (Throwable caught) {
                GWT.log("getTags failed", caught);
                addError(
                    "Internal error fetching item tags: " +
                    caught.getMessage());
            }
        });
    }


    protected void toggleTagHistory ()
    {
        if (_tagHistory != null) {
            if (_content.getWidgetDirection(_tagHistory) == null) {
                _content.add(_tagHistory, DockPanel.EAST);
            } else {
                _content.remove(_tagHistory);
            }
            return;
        }

        _ctx.itemsvc.getTagHistory(_ctx.creds, _itemId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _tagHistory = new FlexTable();
                _tagHistory.setBorderWidth(0);
                _tagHistory.setCellSpacing(0);
                _tagHistory.setCellPadding(2);

                int tRow = 0;
                Iterator iterator = ((Collection) result).iterator();
                while (iterator.hasNext()) {
                    TagHistory history = (TagHistory) iterator.next();
                    String date = history.time.toGMTString();
                    // Fri Sep 29 2006 12:46:12
                    date = date.substring(0, 23);
                    _tagHistory.setText(tRow, 0, date);
                    _tagHistory.setText(tRow, 1, history.member.toString());
                    String actionString;
                    switch(history.action) {
                    case TagHistory.ACTION_ADDED:
                        actionString = "added";
                        break;
                    case TagHistory.ACTION_COPIED:
                        actionString = "copied";
                        break;
                    case TagHistory.ACTION_REMOVED:
                        actionString = "removed";
                        break;
                    default:
                        actionString = "???";
                        break;
                    }
                    _tagHistory.setText(tRow, 2, actionString);
                    _tagHistory.setText(
                        tRow, 3, history.tag == null ? "N/A" : "'" + history.tag + "'");
                    tRow ++;
                }
                _content.add(_tagHistory, DockPanel.EAST);
            }

            public void onFailure (Throwable caught) {
                GWT.log("getTagHistory failed", caught);
                addError("Internal error fetching item tag history: " +
                         caught.getMessage());
            }
        });
    }

    protected void listItem (ItemIdent item)
    {
        _ctx.catalogsvc.listItem(_ctx.creds, item, new AsyncCallback() {
            public void onSuccess (Object result) {
                _status.setText("Item listed.");
            }
            public void onFailure (Throwable caught) {
                String reason = caught.getMessage();
                _status.setText("Item listing failed: " + reason);
            }
        });
    }

    protected void remixItem (ItemIdent item)
    {
        _ctx.itemsvc.remixItem(_ctx.creds, item, new AsyncCallback() {
            public void onSuccess (Object result) {
                // TODO: update display
                _status.setText("Item remixed.");
            }
            public void onFailure (Throwable caught) {
                String reason = caught.getMessage();
                _status.setText("Item remixing failed: " + reason);
            }
        });
    }

    protected void addError (String error)
    {
        _errorContainer.add(new Label(error));
    }

    protected void clearErrors ()
    {
        _errorContainer.clear();
    }

    protected WebContext _ctx;
    protected ItemIdent _itemId;
    protected Item _item;
    protected ItemDetail _itemDetail;

    protected ItemPanel _parent;
    protected HeaderValueTable _table;

    protected DockPanel _content;
    protected VerticalPanel _errorContainer;
    protected FlowPanel _tagContainer;
    protected ListBox _historicalTags;

    protected FlexTable _tagHistory;
    protected ItemRating _ratingImage;
    protected Label _status;
}
