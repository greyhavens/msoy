//
// $Id$

package client.mail;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import client.util.HeaderValueTable;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.MailHeaders;
import com.threerings.msoy.web.data.MailMessage;

public class MailApplication extends DockPanel
{
    public MailApplication (WebContext ctx)
    {
        super();
        _ctx = ctx;
        setStyleName("mailApp");
        setSpacing(5);
        VerticalPanel sideBar = new VerticalPanel();
        sideBar.setStyleName("mailFolders");
        Button composeButton = new Button("Compose");
        composeButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                new MailComposition(_ctx, 2, "").show();
            }
        });
        sideBar.add(composeButton);

        _folderPanel = new VerticalPanel();
        sideBar.add(_folderPanel);
        add(sideBar, DockPanel.WEST);

        _headerPanel = new SimplePanel();
        _headerPanel.setStyleName("mailHeaders");
        _headerPanel.setVisible(true); // TODO TO DO TODO
        add(_headerPanel, DockPanel.NORTH);
        setCellWidth(_headerPanel, "100%");
        
        _messagePanel = new VerticalPanel();
        _messagePanel.setStyleName("mailMessage");
        _messagePanel.setVisible(false);
        add(_messagePanel, DockPanel.CENTER);
        setCellWidth(_messagePanel, "100%");
        
        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("groupDetailErrors");
        add(_errorContainer, DockPanel.SOUTH);
    }

    public void onLoad ()
    {
        _currentFolder = MailFolder.INBOX_FOLDER_ID;
        loadFolders();
        _currentMessage = -1;
    }
    
    public void showFolder (int folderId)
    {
        _currentFolder = folderId;
        loadHeaders();
    }

    public void showMessage (int messageId)
    {
        if (_currentFolder < 0) {
            addError("Internal error: asked to display a message, but no folder selected.");
            return;
        }
        _currentMessage = messageId;
        loadMessage();
    }

    protected void loadFolders ()
    {
        _ctx.mailsvc.getFolders(_ctx.creds, new AsyncCallback() {
            public void onSuccess (Object result) {
                _folders = (List) result;
                if (_currentFolder < 0) {
                    _currentFolder = MailFolder.INBOX_FOLDER_ID;
                    loadHeaders();
                }
                refreshFolderPanel();
            }
            public void onFailure (Throwable caught) {
                addError("Failed to fetch mail folders from database: " + caught.getMessage());
            }
            
        });
    }
    
    protected void loadHeaders ()
    {
        if (_currentFolder < 0) {
            // should not happen, needless to say
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
    
    protected void refreshFolderPanel ()
    {
        _folderPanel.clear();
        Iterator i = _folders.iterator();
        while (i.hasNext()) {
            MailFolder folder = (MailFolder) i.next();
            _folderPanel.add(new Hyperlink(folder.name, "f" + folder.folderId));
            // TODO: Add total/unread count.
        }
    }

    protected void refreshHeaderPanel ()
    {
        _messagePanel.setVisible(false);
        _headerPanel.clear();
        _headerPanel.setVisible(true);
        FlexTable table = new FlexTable();
        table.setWidth("100%");
        _headerPanel.setWidget(table);
        CellFormatter formatter = table.getCellFormatter();
        int row = 0;

        Iterator i = _headers.iterator();
        while (i.hasNext()) {
            MailHeaders headers = (MailHeaders) i.next();
            Widget link = new Hyperlink(
                headers.subject, "f" + _currentFolder + ":" + headers.messageId);
            table.setWidget(row, 0, link);
            formatter.setStyleName(row, 0, "mailRowSubject");
            table.setText(row, 1, headers.sender.memberName);
            formatter.setStyleName(row, 1, "mailRowSender");
            table.setText(row, 2, formatDate(headers.sent));
            formatter.setStyleName(row, 2, "mailRowDate");
            row ++;
        }
    }

    protected void refreshMessagePanel ()
    {
        _messagePanel.clear();
        _messagePanel.setVisible(true);
        HeaderValueTable headers = new HeaderValueTable();
        headers.setStyleName("mailMessageHeaders");
        headers.addRow("From", _message.headers.sender.memberName);
        headers.addRow("Date", _message.headers.sent.toString().substring(0, 21));
        headers.addRow("Subject", _message.headers.subject);
        _messagePanel.add(headers);
        _messagePanel.setCellWidth(headers, "100%");

        HorizontalPanel buttonBox = new HorizontalPanel();
        buttonBox.setStyleName("mailMessageButtons");
        Button replyButton = new Button("Reply");
        replyButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                String subject = _message.headers.subject;
                if (subject.length() < 3 || !subject.substring(0, 3).equalsIgnoreCase("re:")) {
                    subject = "re: " + subject;
                }
                new MailComposition(_ctx, _message.headers.sender, subject).show();
            }
        });
        buttonBox.add(replyButton);
        _messagePanel.add(buttonBox);
        
        SimplePanel messagePanel = new SimplePanel();
        messagePanel.setStyleName("mailMessageBody");
        _messagePanel.add(messagePanel);
        messagePanel.setWidget(textToHTML(_message.message));
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
    protected int _currentFolder;
    protected int _currentMessage;
    
    protected VerticalPanel _folderPanel;
    protected SimplePanel _headerPanel;
    protected VerticalPanel _messagePanel;
    protected VerticalPanel _errorContainer;
}
