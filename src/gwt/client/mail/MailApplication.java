//
// $Id$

package client.mail;

import java.util.Iterator;
import java.util.List;

import client.util.HeaderValueTable;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
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
        setHeight("100%");
        setWidth("100%");
        
        _folderPanel = new ScrollPanel();
        _folderPanel.setWidth("25%");
        add(_folderPanel, DockPanel.WEST);
        
        _headerPanel = new ScrollPanel();
        _headerPanel.setHeight("25%");
        add(_headerPanel, DockPanel.NORTH);
        
        _messagePanel = new ScrollPanel();
        add(_messagePanel, DockPanel.CENTER);
        
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
        if (folderId != _currentFolder) {
            _currentFolder = folderId;
            refreshHeaderPanel();
        }
    }

    public void showMessage (int messageId)
    {
        if (_currentFolder < 0) {
            addError("Internal error: asked to display a message, but no folder selected.");
            return;
        }
        if (_currentMessage != messageId) {
            _currentMessage = messageId;
            loadMessage();
        }
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
        FlexTable table = new FlexTable();
        _folderPanel.setWidget(table);
        int row = 0;

        Iterator i = _folders.iterator();
        while (i.hasNext()) {
            MailFolder folder = (MailFolder) i.next();
            table.setWidget(row, 0, new Hyperlink(folder.name, "f" + folder.folderId));
            // TODO: Add total/unread count.
            row ++;
        }
    }

    protected void refreshHeaderPanel ()
    {
        FlexTable table = new FlexTable();
        _headerPanel.setWidget(table);
        CellFormatter formatter = table.getCellFormatter();
        int row = 0;

        Iterator i = _headers.iterator();
        while (i.hasNext()) {
            MailHeaders headers = (MailHeaders) i.next();
            Widget link = new Hyperlink(
                headers.subject, "f" + _currentFolder + ":" + _currentMessage);
            table.setWidget(row, 0, link);
            formatter.setWidth(0, 0, "25%");
            table.setText(row, 1, headers.sender.memberName);
            formatter.setWidth(0, 1, "60%");
            table.setText(row, 2, headers.sent.toString().substring(0, 20));
            formatter.setWidth(0, 2, "15%");
            row ++;
        }
    }

    protected void refreshMessagePanel ()
    {
        VerticalPanel panel = new VerticalPanel();
        _messagePanel.setWidget(panel);

        HeaderValueTable headers = new HeaderValueTable();
        headers.addRow("Subject", _message.headers.subject);
        headers.addRow("From", _message.headers.sender.memberName);
        headers.addRow("Date", _message.headers.sent.toString().substring(0, 20));
        headers.addRow("To", _message.headers.recipient.memberName);
        panel.add(headers);

        HorizontalPanel buttonBox = new HorizontalPanel();
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
        panel.add(buttonBox);
        
        SimplePanel message = new SimplePanel();
        message.setWidget(new Label(_message.message));
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
    
    protected ScrollPanel _folderPanel;
    protected ScrollPanel _headerPanel;
    protected ScrollPanel _messagePanel;
    protected VerticalPanel _errorContainer;
}
