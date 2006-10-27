//
// $Id$

package client.mail;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import client.util.HeaderValueTable;
import client.util.InlineLabel;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.HTMLTable.RowFormatter;
import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.MailHeaders;
import com.threerings.msoy.web.data.MailMessage;

public class MailApplication extends DockPanel
    implements PopupListener
{
    private static final int HEADER_ROWS = 10;
    public MailApplication (WebContext ctx)
    {
        super();
        _ctx = ctx;
        setStyleName("mailApp");
        setSpacing(5);
        
        VerticalPanel sideBar = new VerticalPanel();
        sideBar.setStyleName("mailFolders");
        sideBar.setSpacing(5);

        Button composeButton = new Button("Compose");
        composeButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                MailComposition composition = new MailComposition(_ctx, 2, "");
                composition.addPopupListener(MailApplication.this);
                composition.show();
            }
        });
        sideBar.add(composeButton);

        Button refreshButton = new Button("Refresh");
        refreshButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                refresh();
            }
        });
        sideBar.add(refreshButton);

        _folderContainer = new SimplePanel();
        sideBar.add(_folderContainer);
        add(sideBar, DockPanel.WEST);

        _headerContainer = new SimplePanel();
        add(_headerContainer, DockPanel.NORTH);
        setCellWidth(_headerContainer, "100%");

        _messageContainer = new SimplePanel();
        add(_messageContainer, DockPanel.CENTER);
        setCellWidth(_messageContainer, "100%");
        
        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("groupDetailErrors");
        add(_errorContainer, DockPanel.SOUTH);
        
        loadFolders();
    }

    public void onPopupClosed (PopupPanel sender, boolean autoClosed)
    {
        refresh();
    }

    public void show (int folderId, int headerOffset, int messageId)
    {
        _currentFolder = folderId;
        _currentOffset = headerOffset;
        _currentMessage = messageId;
        updateHistory();
        loadHeaders();
        if (messageId >= 0) {
            loadMessage();
        } else {
            _messageContainer.clear();
        }
    }

    public void refresh ()
    {
        _currentMessage = -1;
        _messageContainer.clear();
        loadFolders();
        if (_currentFolder >= 0) {
            loadHeaders();
        } else {
            _headerContainer.clear();
        }
    }
    
    protected void loadFolders ()
    {
        _ctx.mailsvc.getFolders(_ctx.creds, new AsyncCallback() {
            public void onSuccess (Object result) {
                _folders = (List) result;
                refreshFolderPanel();
            }
            public void onFailure (Throwable caught) {
                addError("Failed to fetch mail folders from database: " + caught.getMessage());
            }
        });
    }
    
    protected void refreshFolderPanel ()
    {
        VerticalPanel folderList = new VerticalPanel();
        Iterator i = _folders.iterator();
        while (i.hasNext()) {
            MailFolder folder = (MailFolder) i.next();
            String name = folder.name;
            if (folder.unreadCount > 0) {
                name += " (" + folder.unreadCount + ")";
            }
            Hyperlink link = new Hyperlink(name, "f" + folder.folderId);
            link.setStyleName("mailFolderEntry");
            if (folder.unreadCount > 0) {
                link.addStyleName("unread");
            }
            folderList.add(link);
        }
        _folderContainer.setWidget(folderList);
    }

    protected void loadHeaders ()
    {
        if (_currentFolder < 0) {
            addError("Internal error: asked to load headers, but no folder selected.");
            return;
        }
        _ctx.mailsvc.getHeaders(_ctx.creds, _currentFolder, new AsyncCallback() {
            public void onSuccess (Object result) {
                _headers = (List) result;
                refreshHeaderPanel();
            }
            public void onFailure (Throwable caught) {
                addError("Failed to fetch mail headers from database: " + caught.getMessage());
            }
            
        });
    }
    
    protected void refreshHeaderPanel ()
    {
        VerticalPanel headerPanel = new VerticalPanel();
        headerPanel.setWidth("100%");
        if (_headers.size() > 0) {
            FlexTable table = new FlexTable();
            table.setStyleName("mailHeaders");
            table.setWidth("100%");
            int row = 0;
            CellFormatter cellFormatter = table.getCellFormatter();
            RowFormatter rowFormatter = table.getRowFormatter();

            // initialize our collection of currently checked messages
            _checkedMessages = new HashSet();

            int lastMsg = Math.min(_headers.size(), _currentOffset + HEADER_ROWS);
            for (int msg = _currentOffset; msg < lastMsg; msg ++) {
                int col = 0;
                final MailHeaders headers = (MailHeaders) _headers.get(msg);

                CheckBox cBox = new CheckBox();
                cBox.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        if (sender instanceof CheckBox) {
                            if (((CheckBox) sender).isChecked()) {
                                _checkedMessages.add(headers);
                                _massDelete.setEnabled(true);
                            } else {
                                _checkedMessages.remove(headers);
                                _massDelete.setEnabled(_checkedMessages.size() > 0);
                            }
                        }
                    }
                });
                table.setWidget(row, col, cBox);
                cellFormatter.setStyleName(row, col, "mailRowCheckbox");
                col ++;

                Widget link = new Hyperlink(headers.subject, "f" + _currentFolder + "." +
                                            _currentOffset + "." + headers.messageId);
                table.setWidget(row, col, link);
                cellFormatter.setStyleName(row, col, "mailRowSubject");
                col ++;
                table.setText(row, col, headers.sender.memberName);
                cellFormatter.setStyleName(row, col, "mailRowSender");
                col ++;
                table.setText(row, col, formatDate(headers.sent));
                cellFormatter.setStyleName(row, col, "mailRowDate");
                col ++;
                rowFormatter.setStyleName(row, "mailRow");
                if (headers.unread) {
                    rowFormatter.addStyleName(row, "unread");
                }
                row ++;
            }
            headerPanel.add(table);
            
            HorizontalPanel controlBox = new HorizontalPanel();
            controlBox.setWidth("100%");
            controlBox.setSpacing(5);
            _massDelete = new Button("Delete Checked Messages");
            _massDelete.setEnabled(false);
            _massDelete.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    deleteMessages(_checkedMessages.toArray());
                    _massDelete.setEnabled(false);
                }
            });
            controlBox.add(_massDelete);
            HorizontalPanel pager = new HorizontalPanel();
            pager.setSpacing(3);
            pager.setStyleName("mailHeaderPager");
            if (_currentOffset > 0) {
                Label left = new InlineLabel ("<<");
                left.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        _currentOffset = Math.max(0, _currentOffset - HEADER_ROWS);
                        refreshHeaderPanel();
                        updateHistory();
                    }
                });
                pager.add(left);
            }
            Label text = new InlineLabel(
                (_currentOffset + 1) + "-" + lastMsg + " of " + _headers.size());
            pager.add(text);
            if (_currentOffset + HEADER_ROWS < _headers.size()) {
                Label right = new InlineLabel (">>");
                right.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        _currentOffset += HEADER_ROWS;
                        refreshHeaderPanel();
                        updateHistory();
                    }
                });
                pager.add(right);
            }
            controlBox.add(pager);
            headerPanel.add(controlBox);
        }
        _headerContainer.setWidget(headerPanel);
    }

    protected void loadMessage ()
    {
        if (_currentFolder < 0) {
            addError("Internal error: asked to load a message, but no folder selected.");
            return;
        }
        if (_currentMessage < 0) {
            return;
        }
        _ctx.mailsvc.getMessage(_ctx.creds, _currentFolder, _currentMessage, new AsyncCallback() {
            public void onSuccess (Object result) {
                _message = (MailMessage) result;
                refreshMessagePanel();
            }
            public void onFailure (Throwable caught) {
                addError("Failed to fetch mail message from database: " + caught.getMessage());
            }
            
        });
    }

    protected void refreshMessagePanel ()
    {
        VerticalPanel messagePanel = new VerticalPanel();
        messagePanel.setStyleName("mailMessage");
        HeaderValueTable headers = new HeaderValueTable();
        headers.setStyleName("mailMessageHeaders");
        headers.addRow("From", _message.headers.sender.memberName);
        headers.addRow("Date", _message.headers.sent.toString().substring(0, 21));
        headers.addRow("Subject", _message.headers.subject);
        messagePanel.add(headers);
        messagePanel.setCellWidth(headers, "100%");

        HorizontalPanel buttonBox = new HorizontalPanel();
        buttonBox.setStyleName("mailMessageButtons");
        Button replyButton = new Button("Reply");
        replyButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                String subject = _message.headers.subject;
                if (subject.length() < 3 || !subject.substring(0, 3).equalsIgnoreCase("re:")) {
                    subject = "re: " + subject;
                }
                MailComposition composition =
                    new MailComposition(_ctx, _message.headers.sender, subject);
                composition.addPopupListener(MailApplication.this);
                composition.show();
            }
        });
        buttonBox.add(replyButton);
        final Button deleteButton = new Button("Delete");
        deleteButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                deleteButton.setEnabled(false);
                deleteMessages(new MailHeaders[] { _message.headers });
            }
        });
        buttonBox.add(deleteButton);
        messagePanel.add(buttonBox);
        
        SimplePanel messageBody = new SimplePanel();
        messageBody.setStyleName("mailMessageBody");
        messageBody.setWidget(textToHTML(_message.message));
        messagePanel.add(messageBody);

        _messageContainer.setWidget(messagePanel);
    }

    // since collection-to-array type checking is a lost cause anyway, just accept Object[] here
    protected void deleteMessages (Object[] objects)
    {
        if (objects.length == 0) {
            return;
        }
        int folderId = ((MailHeaders) objects[0]).folderId;
        int msgIds[] = new int[objects.length];
        for (int i = 0; i < objects.length; i ++) {
            msgIds[i] = ((MailHeaders) objects[i]).messageId;
        }
        _ctx.mailsvc.deleteMessages(_ctx.creds, folderId, msgIds, new AsyncCallback() {
            public void onSuccess (Object result) {
                refresh();
            }
            public void onFailure (Throwable caught) {
                addError("Failed to delete messages: " + caught.getMessage());
            }
        });
    }

    protected void updateHistory ()
    {
        if (_currentMessage >= 0) {
            History.newItem("f" + _currentFolder + "." + _currentOffset + "." + _currentMessage);
        } else {
            History.newItem("f" + _currentFolder + "." + _currentOffset);
        }
    }

    // scan some text and generate HTML to deal with it
    protected Widget textToHTML (String message)
    {
        StringBuffer html = new StringBuffer();
        boolean collectSpaces = true;
        for (int i = 0; i < message.length(); i ++) {
            String bit;
            char c = message.charAt(i);
            switch(c) {
            case '\r':
                // completely ignore
                continue;
            case '<':
                bit = "&lt;";
                collectSpaces = false;
                break;
            case '>':
                bit = "&gt;";
                collectSpaces = false;
                break;
            case '&':
                bit = "&amp;";
                collectSpaces = false;
                break;
            case '\n':
                bit = "<br>\n";
                collectSpaces = true;
                break;
            case ' ': case '\t':
                if (!collectSpaces) {
                    collectSpaces = true;
                    bit = null;
                    break;
                }
                bit = "&nbsp;";
                break;
            default:
                collectSpaces = false;
                bit = null;
                break;
            }
            if (bit != null) {
                html.append(bit);
            } else {
                html.append(c);
            }
        }
        return new HTML(html.toString());
    }

    // Date.toString() returns: Wed Oct 25 2006 15:30:32 GMT-0500 (CDT)
    protected String formatDate(Date date)
    {
        long nowTime = System.currentTimeMillis();
        Date now = new Date(nowTime);
        if (now.getYear() != date.getYear()) {
            // e.g. 25/10/06
            return date.getDay() + "/" + date.getMonth() + "/" + date.getYear();
        }
        int hourDiff = (int) (nowTime - date.getTime()) / (3600 * 1000);
        if (hourDiff > 6*24) {
            // e.g. Oct 25
            return date.toString().substring(4, 10);
        }
        if (hourDiff > 23) {
            // e.g. Wed 15:10
            String str = date.toString();
            return str.substring(0, 3) + " " + str.substring(16, 21);
        }
        // e.g. 15:10
        return date.toString().substring(16, 21);
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

    protected List _folders;
    protected List _headers;
    protected MailMessage _message;
    protected Set _checkedMessages;
    protected int _currentFolder;
    protected int _currentOffset;
    protected int _currentMessage;

    protected Button _massDelete;
    
    protected SimplePanel _folderContainer;
    protected SimplePanel _headerContainer;
    protected SimplePanel _messageContainer;
    protected VerticalPanel _errorContainer;
}
