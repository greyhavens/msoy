//
// $Id$

package client.inventory;

import java.util.Collection;
import java.util.Iterator;

import client.MsoyEntryPoint;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ComplexPanel;
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
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;

import com.threerings.msoy.item.web.Document;
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
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

        setStyleName("itemDetailPopup");

        _table = new FlexTable();
        _table.setBorderWidth(0);
        _table.setCellSpacing(0);
        _table.setCellPadding(3);
        setWidget(_table);
        _row = 0;

        if (_item.parentId != -1) {
            addHeader("Clone Information");
            addRow("Clone ID", String.valueOf(_item.itemId),
                   "Owner", String.valueOf(_item.ownerId));
        }
        addHeader("Generic Item Information");
        addRow("Item ID", String.valueOf(_item.getProgenitorId()),
               "Flag mask", String.valueOf(_item.flags));
        // TODO: Should be MemberGNames
        addRow("Owner ID", String.valueOf(_item.ownerId),
               "Creator ID", String.valueOf(_item.creatorId));
        Widget thumbContainer =
            ItemContainer.createContainer(MsoyEntryPoint.toMediaPath(
                _item.getThumbnailPath()));
        Widget furniContainer =
            ItemContainer.createContainer(MsoyEntryPoint.toMediaPath(
                _item.getFurniMedia().getMediaPath()));
        addRow("Thumbnail", thumbContainer,
               "Furniture", furniContainer);
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

        } else if (_item instanceof Game) {
            addHeader("Game Information");
            addRow("Name", ((Game)_item).name,
                   "# Players (Desired)",
                   String.valueOf(((Game)_item).desiredPlayers));
            addRow("# Players (Minimum)",
                   String.valueOf(((Game)_item).minPlayers),
                   "# Players (Maximum)",
                   String.valueOf(((Game)_item).maxPlayers));

        } else if (_item instanceof Photo) {
            addHeader("Photo Information");
            Widget photoContainer =
                ItemContainer.createContainer(MsoyEntryPoint.toMediaPath(
                    ((Photo)_item).photoMedia.getMediaPath()));
            addRow("Photo Media", photoContainer);
            addRow("Caption", ((Photo)_item).caption);

        } else {
            addHeader("UNKNOWN OBJECT TYPE");
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
                _ctx.itemsvc.tagItem(
                    _ctx.creds, _itemId,
                    ((TextBox) sender).getText(),
                    new AsyncCallback() {
                        public void onSuccess (Object result) {
                            updateTags();
                        }
                        public void onFailure (Throwable caught) {
                            // TODO: generalize error handling
                        }
                    });
                ((TextBox) sender).setText(null);
            }
        });

        _tagHistory = new ListBox();
        _tagHistory.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                ListBox box = (ListBox) sender;
                _ctx.itemsvc.tagItem(
                    _ctx.creds, _itemId,
                    box.getValue(box.getSelectedIndex()),
                    new AsyncCallback() {
                        public void onSuccess (Object result) {
                            updateTags();
                        }
                        public void onFailure (Throwable caught) {
                            // TODO: generalize error handling
                        }
                    });
            }
        });

        _tagContainer = new FlowPanel();
        updateTags();
        ComplexPanel enterTagContainer = new HorizontalPanel();
        enterTagContainer.add(newTagBox);
        enterTagContainer.add(_tagHistory);
        addRow("Enter a new tag", enterTagContainer);
        addRow("Tags", _tagContainer);
    }

    protected void updateTags ()
    {
        _ctx.itemsvc.getTagHistory(
            _ctx.creds, _ctx.creds.memberId, new AsyncCallback() {
                public void onSuccess (Object result) {
                    _tagHistory.clear();
                    Iterator i = ((Collection) result).iterator();
                    while (i.hasNext()) {
                        TagHistory history = (TagHistory) i.next();
                        if (history.member.memberId == _ctx.creds.memberId) {
                            if (history.tag != null) {
                                _tagHistory.addItem(history.tag);
                            }
                        }
                    }
                    _tagHistory.setVisible(_tagHistory.getItemCount() > 0);
                }
                public void onFailure (Throwable caught) {
                    // TODO: generalize error handling
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
                _tagContainer.add(new Label(builder.toString()));
            }
            public void onFailure (Throwable caught) {
            }
        });


    }
    protected void updateRatingContainer (final boolean edit)
    {
        _ctx.itemsvc.getRating(
            _ctx.creds, _itemId,
            _ctx.creds.memberId, new AsyncCallback() {
                public void onSuccess (Object result) {
                    fillRatingContainer(((Byte)result).byteValue(), edit);
                }
                public void onFailure (Throwable caught) {
                    GWT.log("getRating failed", caught);
                    // TODO: for now, handle all async errors this way
                    _ratingContainer.add(
                        new Label("[Error: " + caught.getMessage() + "]"));
                    return;
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
                                // TODO: if ServiceException, translate
                                _ratingContainer.add(
                                    new Label("[Error: " + caught.getMessage() + "]"));
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

    protected WebContext _ctx;
    protected ItemIdent _itemId;
    protected Item _item;

    protected FlexTable _table;
    protected int _row;

    protected ComplexPanel _ratingContainer;
    protected ComplexPanel _tagContainer;
    protected ListBox _tagHistory;
    protected int _ratingRow;
}
