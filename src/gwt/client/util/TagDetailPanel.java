//
// $Id$

package client.util;

import client.shell.CShell;

import java.util.Collection;
import java.util.Iterator;

import client.util.PromptPopup;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.TagHistory;

/**
 * Displays tagging information for a particular item.
 */
public class TagDetailPanel extends FlexTable
{
    /** Interface to the interaction between this panel and the service handling tagging/flagging
     * in the background */
    public interface TagService 
    {
        public void tag (String tag, AsyncCallback callback);
        public void untag (String tag, AsyncCallback callback);
        public void getRecentTags (AsyncCallback callback);
        public void getTags (AsyncCallback callback);
        public boolean supportFlags ();
        /** 
         * In this case, the implementor is responsible for editing the flags on the local
         * object that is being flagged, and is therefore responsible for providing the callback.
         * 
         * @param flag the flag to send to the server and set on the local object on success
         * @param statusLabel A label to set with an error message on failure
         */
        public void setFlags (byte flag, Label statusLabel);

    public TagDetailPanel (TagService service)
    {
        setStyleName("tagDetailPanel");
        _service = service;

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
                _service.tag(tagName, new AsyncCallback() {
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
        _quickTags = new ListBox();
        _quickTags.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                ListBox box = (ListBox) sender;
                String value = box.getValue(box.getSelectedIndex());
                _service.tag(value, new AsyncCallback() {
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
        setWidget(1, 3, _quickTags);

        if (_service.supportFlags()) {
            final PopupPanel menuPanel = new PopupPanel(true);
            MenuBar menu = new MenuBar(true);
            menu.addItem(getMenuItem("Mature", Item.FLAG_FLAGGED_MATURE, menuPanel));
            menu.addItem(getMenuItem("Copyright Violation", Item.FLAG_FLAGGED_COPYRIGHT, 
                menuPanel));
            menuPanel.add(menu);
            final InlineLabel flagLabel = new InlineLabel("Flag");
            flagLabel.addStyleName("LabelLink");
            // use a MouseListener instead of ClickListener so we can get at the mouse (x,y)
            flagLabel.addMouseListener(new MouseListener() {
                public void onMouseDown (Widget sender, int x, int y) { 
                    menuPanel.setPopupPosition(flagLabel.getAbsoluteLeft() + x, 
                            flagLabel.getAbsoluteTop() + y);
                    menuPanel.show();
                }
                public void onMouseLeave (Widget sender) { }
                public void onMouseUp (Widget sender, int x, int y) { }
                public void onMouseEnter (Widget sender) { }
                public void onMouseMove (Widget sender, int x, int y) { }
            });
            setWidget(1, 4, flagLabel);
        }

        setWidget(2, 0, _status = new Label(""));

        getFlexCellFormatter().setColSpan(0, 0, getCellCount(1));
        getFlexCellFormatter().setColSpan(2, 0, getCellCount(1));

        refreshTags();
    }

    protected MenuItem getMenuItem (final String menuLabel, final byte flag,
                                    final PopupPanel parent)
    {
        // TODO: raw HTML is a bit yuck; maybe add more structure to PromptPopup 
        final String text =
            "<b>Flag item as " + menuLabel + "?</b><br>\n" +
            "<hr>\n" +
            "Blah blah blah blah.\n";

        MenuItem mature = new MenuItem(menuLabel, new Command() {
            public void execute() {
                (new PromptPopup(text, "Flag", "Cancel") {
                    public void onAffirmative () {
                        parent.hide();
                        updateItemFlags(flag);
                    }
                    public void onNegative () { }
                }).prompt();
            }
        });
        return mature;
    }

    protected void updateItemFlags (final byte flag)
    {
        _service.setFlags(flag, _status);
    }
    
    protected void toggleTagHistory ()
    {
        // TODO: if this is used again, it will need to be abstracted like everything else in this
        // class
//         if (_tagHistory != null) {
//             if (_content.getWidgetDirection(_tagHistory) == null) {
//                 _content.add(_tagHistory, DockPanel.EAST);
//             } else {
//                 _content.remove(_tagHistory);
//             }
//             return;
//         }

//         CItem.itemsvc.getTagHistory(CItem.creds, _itemId, new AsyncCallback() {
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
        if (CShell.creds != null) {
            _service.getRecentTags(new AsyncCallback() {
                public void onSuccess (Object result) {
                    _quickTags.clear();
                    Iterator i = ((Collection) result).iterator();
                    while (i.hasNext()) {
                        TagHistory history = (TagHistory) i.next();
                        if (history.member.getMemberId() == CShell.getMemberId()) {
                            if (history.tag != null) {
                                _quickTags.addItem(history.tag);
                            }
                        }
                    }
                    _quickTags.setVisible(_quickTags.getItemCount() > 0);
                }
                public void onFailure (Throwable caught) {
                    GWT.log("getTagHistory failed", caught);
                    _status.setText("Internal error fetching tag history: " + caught.getMessage());
                }
            });
        }

        _service.getTags(new AsyncCallback() {
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

    protected TagService _service;

    protected Label _tags, _status;
    protected ListBox _quickTags;
    protected FlexTable _tagHistory;
}
