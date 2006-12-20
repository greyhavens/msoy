//
// $Id$

package client.item;

import java.util.Collection;
import java.util.Iterator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.TagHistory;

import client.util.WebContext;

/**
 * Displays tagging information for a particular item.
 */
public class TagDetailPanel extends FlexTable
{
    public TagDetailPanel (WebContext ctx, Item item)
    {
        setStyleName("tagDetailPanel");
        _ctx = ctx;
        _item = item;

        setWidget(0, 0, _tags = new Label("Loading..."));

        setWidget(1, 0, new Label("Add tag:"));
        TextBox newTagBox = new TextBox();
        newTagBox.setMaxLength(20);
        newTagBox.setVisibleLength(12);
        newTagBox.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                String tagName = ((TextBox) sender).getText().trim().toLowerCase();
                if (tagName.length() == 0) {
                    return;
                }
                if (tagName.length() > 24) {
                    _status.setText("Invalid tag: can't be more than 24 characters.");
                    return;
                }
                for (int ii = 0; ii < tagName.length(); ii ++) {
                    char c = tagName.charAt(ii);
                    if (Character.isLetter(c) || Character.isDigit(c) || c == '_') {
                        continue;
                    }
                    _status.setText("Invalid tag: use letters, numbers, and underscore.");
                    return;
                }
                _ctx.itemsvc.tagItem(
                    _ctx.creds, _item.getIdent(), tagName, true, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        refreshTags();
                    }
                    public void onFailure (Throwable caught) {
                        GWT.log("tagItem failed", caught);
                        _status.setText("Internal error adding tag: " + caught.getMessage());
                    }
                });
                ((TextBox) sender).setText(null);
            }
        }));
        setWidget(1, 1, newTagBox);

        setWidget(1, 2, new Label("Quick add:"));
        _historicalTags = new ListBox();
        _historicalTags.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                ListBox box = (ListBox) sender;
                String value = box.getValue(box.getSelectedIndex());
                _ctx.itemsvc.tagItem(
                    _ctx.creds, _item.getIdent(), value, true, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        refreshTags();
                    }
                    public void onFailure (Throwable caught) {
                        GWT.log("tagItem failed", caught);
                        _status.setText("Internal error adding tag: " + caught.getMessage());
                    }
                });
            }
        });
        setWidget(1, 3, _historicalTags);

        setWidget(2, 0, _status = new Label(""));

        getFlexCellFormatter().setColSpan(0, 0, getCellCount(1));
        getFlexCellFormatter().setColSpan(2, 0, getCellCount(1));

        refreshTags();
    }

    protected void toggleTagHistory ()
    {
//         if (_tagHistory != null) {
//             if (_content.getWidgetDirection(_tagHistory) == null) {
//                 _content.add(_tagHistory, DockPanel.EAST);
//             } else {
//                 _content.remove(_tagHistory);
//             }
//             return;
//         }

//         _ctx.itemsvc.getTagHistory(_ctx.creds, _itemId, new AsyncCallback() {
//             public void onSuccess (Object result) {
//                 _tagHistory = new FlexTable();
//                 _tagHistory.setBorderWidth(0);
//                 _tagHistory.setCellSpacing(0);
//                 _tagHistory.setCellPadding(2);

//                 int tRow = 0;
//                 Iterator iterator = ((Collection) result).iterator();
//                 while (iterator.hasNext()) {
//                     TagHistory history = (TagHistory) iterator.next();
//                     String date = history.time.toGMTString();
//                     // Fri Sep 29 2006 12:46:12
//                     date = date.substring(0, 23);
//                     _tagHistory.setText(tRow, 0, date);
//                     _tagHistory.setText(tRow, 1, history.member.toString());
//                     String actionString;
//                     switch(history.action) {
//                     case TagHistory.ACTION_ADDED:
//                         actionString = "added";
//                         break;
//                     case TagHistory.ACTION_COPIED:
//                         actionString = "copied";
//                         break;
//                     case TagHistory.ACTION_REMOVED:
//                         actionString = "removed";
//                         break;
//                     default:
//                         actionString = "???";
//                         break;
//                     }
//                     _tagHistory.setText(tRow, 2, actionString);
//                     _tagHistory.setText(
//                         tRow, 3, history.tag == null ? "N/A" : "'" + history.tag + "'");
//                     tRow ++;
//                 }
//                 _content.add(_tagHistory, DockPanel.EAST);
//             }

//             public void onFailure (Throwable caught) {
//                 GWT.log("getTagHistory failed", caught);
//                 _status.setText("Internal error fetching item tag history: " + caught.getMessage());
//             }
//         });
    }

    protected void refreshTags ()
    {
        if (_ctx.creds != null) {
            _ctx.itemsvc.getRecentTags(_ctx.creds, new AsyncCallback() {
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
                    _status.setText("Internal error fetching tag history: " + caught.getMessage());
                }
            });
        }

        _ctx.itemsvc.getTags(_ctx.creds, _item.getIdent(), new AsyncCallback() {
            public void onSuccess (Object result) {
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
                _tags.setText(builder.toString());
            }
            public void onFailure (Throwable caught) {
                _tags.setText("Internal error fetching item tags: " + caught.getMessage());
            }
        });
    }

    protected WebContext _ctx;
    protected Item _item;

    protected Label _tags, _status;
    protected ListBox _historicalTags;
    protected FlexTable _tagHistory;
}
