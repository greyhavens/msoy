//
// $Id$

package client.inventory;

import java.util.Collection;
import java.util.Iterator;

import client.MsoyEntryPoint;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.Document;
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.Pet;
import com.threerings.msoy.item.web.Photo;
import com.threerings.msoy.item.web.TagHistory;
import com.threerings.msoy.web.client.WebContext;

public class ItemDetail extends PopupPanel
{
    public ItemDetail (WebContext ctx, Item item)
    {
        super(true);
        _item = item;
        _ctx = ctx;
        _itemId = new ItemIdent(_item.getType(), _item.getProgenitorId());
        setStyleName("itemPopup");

        setWidget(_content = new DockPanel());

        _table = new FlexTable();
        _table.setBorderWidth(0);
        _table.setCellSpacing(0);
        _table.setCellPadding(3);
        _content.add(_table, DockPanel.CENTER);
        _row = 0;

        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("itemDetailErrors");
        _content.add(_errorContainer, DockPanel.NORTH);

        if (_item.parentId != -1) {
            addHeader("Clone Information");
            addRow("Clone ID", String.valueOf(_item.itemId),
                   "Owner", String.valueOf(_item.ownerId));
        }
        addHeader("Generic Item Information");
        // TODO: flags should be checkboxes when we have some?
        addRow("Item ID", String.valueOf(_item.getProgenitorId()),
               "Flag mask", String.valueOf(_item.flags));
        // TODO: Should be MemberGNames
        addRow("Owner ID", String.valueOf(_item.ownerId),
               "Creator ID", String.valueOf(_item.creatorId));

        Widget thumbWidget = _item.thumbMedia == null ?
            new Label("(default)") :
            ItemContainer.createContainer(
                MsoyEntryPoint.toMediaPath(
                    _item.getThumbnailMedia().getMediaPath()));
        Widget furniWidget = _item.furniMedia == null ?
            new Label("(default)") :
            ItemContainer.createContainer(
                MsoyEntryPoint.toMediaPath(
                    _item.getFurniMedia().getMediaPath()));
        addRow("Thumbnail", thumbWidget, "Furniture", furniWidget);

        // TODO: Maybe merge ItemDetail and ItemEditor, so we could put these
        // TODO: subclass-specific bits into (and rename) e.g. DocumentEditor?
        if (_item instanceof Document) {
            addHeader("Document Information");
            addRow("Title", ((Document)_item).title);
            // we should check if the document has a useful visual
            String url = MsoyEntryPoint.toMediaPath(
                ((Document)_item).docMedia.getMediaPath());
            addRow("Document Media", new HTML(
                "<A HREF='" + url + "'>" + url + "</a>"));

        } else if (_item instanceof Furniture) {
            addHeader("Furniture Information");
            addRow("Action", ((Furniture)_item).action,
                   "Description", ((Furniture)_item).description);

        } else if (_item instanceof Pet) {
            addHeader("Pet Information");
            addRow("Description", ((Pet)_item).description);

        } else if (_item instanceof Game) {
            addHeader("Game Information");
            addRow("Name", ((Game)_item).name,
                   "# Players (Desired)",
                   String.valueOf(((Game)_item).desiredPlayers));
            addRow("# Players (Minimum)",
                   String.valueOf(((Game)_item).minPlayers),
                   "# Players (Maximum)",
                   String.valueOf(((Game)_item).maxPlayers));
            int gameId = _item.getProgenitorId();
            String href = "<a href=\"game.html#" + gameId + "\">";
            addRow("Play", new HTML(href + "Play now</a>"));

        } else if (_item instanceof Photo) {
            addHeader("Photo Information");
            MediaDesc photoMedia = ((Photo)_item).photoMedia;
            Widget photoContainer;
            photoContainer =
                ItemContainer.createContainer(
                    MsoyEntryPoint.toMediaPath(photoMedia.getMediaPath()));
            addRow("Photo", photoContainer);
            addRow("Caption", ((Photo)_item).caption);

        } else if (_item instanceof Avatar) {
            addHeader("Avatar Information");
            MediaDesc avatarMedia = ((Avatar)_item).avatarMedia;
            Widget avatarContainer;
            avatarContainer =
                ItemContainer.createContainer(
                    MsoyEntryPoint.toMediaPath(avatarMedia.getMediaPath()));
            addRow("Description", ((Avatar)_item).description);
            addRow("Avatar", avatarContainer);


        } else {
            addHeader("UNKNOWN OBJECT TYPE: " + _item.getType());
        }

        _ratingContainer = new HorizontalPanel();
        addHeader("Rating Information");
        _ratingRow = _row;
        addRow("Average Rating", String.valueOf(_item.rating),
               "Your Rating", _ratingContainer);
        updateRatingContainer(false);

        addHeader("Tagging Information");

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
                    if (Character.isLetter(c) || !Character.isDigit(c) ||
                        c == '_') {
                        continue;
                    }
                    addError(
                        "Invalid tag: use letters, numbers, and underscore.");
                    return;
                }
                _ctx.itemsvc.tagItem(
                    _ctx.creds, _itemId, tagName,
                    new AsyncCallback() {
                        public void onSuccess (Object result) {
                            updateTags();
                        }
                        public void onFailure (Throwable caught) {
                            GWT.log("tagItem failed", caught);
                            addError(
                                "Internal error adding tag: " +
                                caught.getMessage());
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
                _ctx.itemsvc.tagItem(
                    _ctx.creds, _itemId,
                    box.getValue(box.getSelectedIndex()),
                    new AsyncCallback() {
                        public void onSuccess (Object result) {
                            updateTags();
                        }
                        public void onFailure (Throwable caught) {
                            GWT.log("tagItem failed", caught);
                            addError(
                                "Internal error adding tag: " +
                                caught.getMessage());
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
        addRow("Enter a new tag", enterTagContainer);
        addRow("Tags", _tagContainer);
        updateTags();

        Button button = new Button("Hide/Show");
        button.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                toggleTagHistory();
            }
        });
        addRow("Tagging History", button);
    }

    // @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();

        // center ourselves
        setPopupPosition((Window.getClientWidth() - getOffsetWidth()) / 2,
                         (Window.getClientHeight() - getOffsetHeight()) / 2);
    }

    protected void updateTags ()
    {
        _ctx.itemsvc.getTagHistory(
            _ctx.creds, _ctx.creds.memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _historicalTags.clear();
                Iterator i = ((Collection) result).iterator();
                while (i.hasNext()) {
                    TagHistory history = (TagHistory) i.next();
                    if (history.member.memberId == _ctx.creds.memberId) {
                        if (history.tag != null) {
                            _historicalTags.addItem(history.tag);
                        }
                    }
                }
                _historicalTags.setVisible(
                    _historicalTags.getItemCount() > 0);
            }
            public void onFailure (Throwable caught) {
                GWT.log("getTagHistory failed", caught);
                addError(
                    "Internal error fetching tag history: " +
                    caught.getMessage());
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

    protected void updateRatingContainer (final boolean edit)
    {
        _ctx.itemsvc.getRating(
            _ctx.creds, _itemId, _ctx.creds.memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                fillRatingContainer(((Byte)result).byteValue(), edit);
            }
            public void onFailure (Throwable caught) {
                GWT.log("getRating failed", caught);
                addError("Internal error fetching user's rating: " +
                         caught.getMessage());
            }
        });
    }

    protected void fillRatingContainer (final byte rating, boolean edit)
    {
        _ratingContainer.clear();

        if (edit) {
            // display a dropdown, with a listener that calls back with 'false'
            ListBox box = new ListBox();
            for (int i = 1; i <= 5; i ++) {
                box.addItem(String.valueOf(i));
            }
            box.setVisibleItemCount(1);
            if (rating > 0) {
                box.setSelectedIndex(rating - 1);
            }
            box.addChangeListener(new ChangeListener() {
                public void onChange (Widget sender) {
                    clearErrors();
                    final byte newRating =
                        (byte) (((ListBox)sender).getSelectedIndex()+1);
                    _ctx.itemsvc.rateItem(
                        _ctx.creds, _itemId, newRating,
                        new AsyncCallback() {
                            public void onSuccess (Object result) {
                                _item = (Item) result;
                                _table.setText(_ratingRow, 1,
                                    String.valueOf(_item.rating));
                                fillRatingContainer(newRating, false);
                            }
                            public void onFailure (Throwable caught) {
                                GWT.log("getRating failed", caught);
                                addError(
                                    "Internal error setting user's rating: " +
                                    caught.getMessage());
                            }
                        });
                }
            });
            _ratingContainer.add(box);

        } else {
            // display a text field, with a button that calls back with 'true'
            _ratingContainer.add(new HTML(String.valueOf(rating) + " &nbsp; "));
            // we can rate the item if it's a clone or if it's listed
            if (_item.parentId != -1 || _item.ownerId == -1) {
                Button changeButton = new Button("Change");
                changeButton.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        fillRatingContainer(rating, true);
                    }
                });
                _ratingContainer.add(changeButton);
            }
        }
    }

    private void toggleTagHistory ()
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
                    _tagHistory.setText(
                        tRow, 1, history.member.memberName);
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
                    _tagHistory.setText(tRow, 3,
                                        history.tag == null ?
                                        "N/A" : "'" + history.tag + "'");
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

    protected void addHeader (String header)
    {
        _table.setText(_row, 0, header);
        _table.getFlexCellFormatter().setColSpan(_row, 0, 4);
        _table.getFlexCellFormatter().setAlignment(_row, 0,
            HasHorizontalAlignment.ALIGN_CENTER,
            HasVerticalAlignment.ALIGN_MIDDLE);
        _table.getRowFormatter().setStyleName(_row, "headerRow");
        _row ++;
    }

    protected void addRow (String head, Widget val)
    {
        FlexCellFormatter flexCellFormatter = _table.getFlexCellFormatter();
        _table.setText(_row, 0, head + ":");
        _table.setWidget(_row, 1, val);
        _table.getFlexCellFormatter().setColSpan(_row, 1, 3);
        _table.getRowFormatter().setStyleName(_row, "dataRow");
        flexCellFormatter.setAlignment(
            _row, 0,
            HasHorizontalAlignment.ALIGN_RIGHT,
            HasVerticalAlignment.ALIGN_MIDDLE);
        flexCellFormatter.setAlignment(
            _row, 1,
            HasHorizontalAlignment.ALIGN_LEFT,
            HasVerticalAlignment.ALIGN_MIDDLE);
        _row ++;
    }

    protected void addRow (String head, String val)
    {
        addRow(head, new Label(val));
    }

    protected void addRow (String lhead, Widget lval, String rhead, Widget rval)
    {
        FlexCellFormatter flexCellFormatter = _table.getFlexCellFormatter();
        _table.setText(_row, 0, lhead + ":");
        _table.setWidget(_row, 1, lval);
        _table.setText(_row, 2, rhead + ":");
        _table.setWidget(_row, 3, rval);
        _table.getRowFormatter().setStyleName(_row, "dataRow");
        flexCellFormatter.setAlignment(
            _row, 0,
            HasHorizontalAlignment.ALIGN_RIGHT,
            HasVerticalAlignment.ALIGN_MIDDLE);
        flexCellFormatter.setAlignment(
            _row, 1,
            HasHorizontalAlignment.ALIGN_LEFT,
            HasVerticalAlignment.ALIGN_MIDDLE);
        flexCellFormatter.setAlignment(
            _row, 2,
            HasHorizontalAlignment.ALIGN_RIGHT,
            HasVerticalAlignment.ALIGN_MIDDLE);
        flexCellFormatter.setAlignment(
            _row, 3,
            HasHorizontalAlignment.ALIGN_LEFT,
            HasVerticalAlignment.ALIGN_MIDDLE);
        _row ++;
    }

    protected void addRow (String lhead, String lval, String rhead, Widget rval)
    {
        addRow(lhead, new Label(lval), rhead, rval);
    }

    protected void addRow (String lhead, Widget lval, String rhead, String rval)
    {
        addRow(lhead, lval, rhead, new Label(rval));
    }

    protected void addRow (String lhead, String lval, String rhead, String rval)
    {
        addRow(lhead, new Label(lval), rhead, new Label(rval));
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

    protected FlexTable _table;
    protected int _row;

    protected DockPanel _content;
    protected VerticalPanel _errorContainer;
    protected HorizontalPanel _ratingContainer;
    protected FlowPanel _tagContainer;
    protected ListBox _historicalTags;
    protected int _ratingRow;

    protected FlexTable _tagHistory;
}
