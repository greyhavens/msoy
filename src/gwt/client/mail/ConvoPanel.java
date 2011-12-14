//
// $Id$

package client.mail;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.DateUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.mail.gwt.ConvMessage;
import com.threerings.msoy.mail.gwt.MailService;
import com.threerings.msoy.mail.gwt.MailServiceAsync;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.BorderedDialog;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.util.Link;

/**
 * Displays a single conversation.
 */
public class ConvoPanel extends FlowPanel
{
    public ConvoPanel (ConvosModel model, int convoId)
    {
        setStyleName("convo");
        addStyleName("pagedGrid"); // for our header and footer

        _model = model;
        _convoId = convoId;
        refresh();
    }

    protected void refresh ()
    {
        clear();
        _mailsvc.loadConversation(_convoId, new InfoCallback<MailService.ConvoResult>() {
            public void onSuccess (MailService.ConvoResult result) {
                init(result);
            }
        });
    }

    protected void init (MailService.ConvoResult result)
    {
        boolean review = result.other == null; // the server leaves this null for support reviews
        SmartTable header = new SmartTable("Header", 0, 0);
        header.setWidth("100%");
        header.setHTML(0, 0, "&nbsp;", 1, "TopLeft");
        header.setText(0, 1, review ? _msgs.convoReview() :
            _msgs.convoWith(""+result.other), 1, "Title");

        if (!review) {
            addNavControls(header);
        }
        header.setHTML(0, header.getCellCount(0), "&nbsp;", 1, "TopRight");
        add(header);

        for (int ii = 0; ii < result.messages.size(); ii++) {
            ConvMessage msg = result.messages.get(ii);
            add(new MessageWidget((ii == 0) ? result.subject : null, msg,
                                  msg.sent.getTime() > result.lastRead,
                                  ii == result.messages.size()-1 && !review));
        }

        SmartTable footer = new SmartTable("Footer", 0, 0);
        footer.setWidth("100%");
        footer.setHTML(0, 0, "&nbsp;", 1, "BottomLeft");
        if (!review) {
            // only add the "user controls" if the other user has participated
            for (ConvMessage msg : result.messages) {
                if (msg.author.name.getId() != CShell.getMemberId()) {
                    addUserControls(footer, result.other);
                    break;
                }
            }
        }
        footer.setHTML(0, footer.getCellCount(0), "&nbsp;", 1, "Title"); // spacer
        if (!review) {
            addNavControls(footer);
        }
        footer.setHTML(0, footer.getCellCount(0), "&nbsp;", 1, "BottomRight");
        add(footer);

        // note that we've read this conversation
        _model.markConversationRead(_convoId);
    }

    protected void addNavControls (SmartTable table)
    {
        int col = table.getCellCount(0);

        Button delete = new Button(_msgs.convoDelete());
        new ClickCallback<Boolean>(delete, _msgs.deleteConfirm()) {
            @Override protected boolean callService () {
                _mailsvc.deleteConversation(_convoId, false, this);
                return true;
            }
            @Override protected boolean gotResult (Boolean deleted) {
                if (!deleted) {
                    refresh();
                    MsoyUI.info(_msgs.deleteNotDeleted());
                } else {
                    _model.conversationDeleted(_convoId);
                    History.back();
                }
                return !deleted;
            }
        };
        table.setWidget(0, col++, delete, 1, "Control");

        table.setWidget(0, col++, new Button(_msgs.convoBack(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                History.back();
            }
        }), 1, "Control");
    }

    protected void addUserControls (SmartTable table, MemberName targetName)
    {
        int col = table.getCellCount(0);

        Button complain = new Button(_msgs.convoComplain());
        new ComplainHandler(complain, targetName.toString());
        table.setWidget(0, col++, complain, 1, "Control");

        final int targetId = targetName.getId();
        Button block = new Button(_msgs.convoBlock());
        new ClickCallback<Void>(block, _msgs.convoBlockConfirm()) {
            @Override protected boolean callService () {
                _membersvc.setMuted(CShell.getMemberId(), targetId, true, this);
                return true;
            }

            @Override protected boolean gotResult (Void result) {
                MsoyUI.info(_msgs.convoBlockSuccess());
                return false; // stay disabled
            }
        };
        table.setWidget(0, col++, block, 1, "Control");
    }

    protected class MessageWidget extends SmartTable
    {
        public MessageWidget (String subject, ConvMessage msg, boolean unread, boolean showReply)
        {
            super("Message", 0, 0);
            if (unread) {
                addStyleName("Unread");
            }

            _msg = msg;

            setWidget(0, 0, MsoyUI.createFlowPanel("roleBox", new ThumbBox(msg.author.photo),
                                                   MsoyUI.createRoleLabel(msg.author.role)),
                      1, "Thumb");
            getFlexCellFormatter().setWidth(0, 0, (MediaDescSize.THUMBNAIL_WIDTH+20) + "px");
            getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);

            FlowPanel info = new FlowPanel();
            info.add(Link.memberView(msg.author));
            info.add(MsoyUI.createLabel(DateUtil.formatDateTime(msg.sent), "Sent"));
            setWidget(0, 1, info);
            getFlexCellFormatter().setWidth(0, 1, "150px");
            getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);

            _contents = new FlowPanel();
            if (subject != null) {
                _contents.add(MsoyUI.createLabel(subject, "Subject"));
            }
            if (msg.payload != null) {
                MailPayloadDisplay display = MailPayloadDisplay.getDisplay(_convoId, msg);
                Widget payviz = (CShell.getMemberId() == msg.author.name.getId()) ?
                    display.widgetForSender() : display.widgetForRecipient();
                if (payviz != null) {
                    _contents.add(payviz);
                    _contents.add(WidgetUtil.makeShim(10, 10));
                }
            }
            _contents.add(MsoyUI.createHTML(MailUtil.textToHTML(msg.body), null));
            setWidget(0, 2, _contents, 1, "Body");
            getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);

            if (showReply) {
                _contents.add(WidgetUtil.makeShim(10, 10));
                String action = _msg.author.name.getId() == CShell.getMemberId() ?
                    _msgs.convoFollowUp() : _msgs.convoReply();
                _contents.add(_reply = new Button(action, new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        showReply();
                    }
                }));
            }
        }

        protected void showReply ()
        {
            _contents.remove(_reply);

            final SmartTable reply = new SmartTable(0, 0);
            reply.setWidget(0, 0, _repmsg, 2);
            _repmsg.setVisibleLines(4);
            _repmsg.setWidth("350px");

            reply.setWidget(1, 0, new Button(_cmsgs.cancel(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    _contents.remove(reply);
                    _contents.add(_reply);
                }
            }));

            Button send = new Button(_msgs.convoSend());
            reply.setWidget(1, 1, send);
            reply.getFlexCellFormatter().setHorizontalAlignment(1, 1, HasAlignment.ALIGN_RIGHT);
            _contents.add(reply);

            // wire up our message delivery handler
            new ClickCallback<ConvMessage>(send) {
                @Override protected boolean callService () {
                    // make sure they entered something to send
                    String text = _repmsg.getText().trim();
                    if (text.length() == 0) {
                        return false;
                    }

                    // deliver the message to the recipient
                    _mailsvc.continueConversation(_convoId, text, null, this);
                    return true;
                }

                @Override protected boolean gotResult (ConvMessage sent) {
                    _model.noteMessageAdded(_convoId, sent);
                    _contents.remove(reply);
                    ConvoPanel.this.insert(new MessageWidget(null, sent, false, true),
                                           ConvoPanel.this.getWidgetCount()-1);
                    return false;
                }
            };

            _repmsg.setFocus(true);
        }

        protected ConvMessage _msg;
        protected FlowPanel _contents;
        protected Button _reply;
        protected TextArea _repmsg = new TextArea();
    }

    protected class ComplainHandler extends ClickCallback<Void>
    {
        public ComplainHandler (Button trigger, String targetName)
        {
            super(trigger, ""); // kludge to ensure call of displayConfirmPopup
            _targetName = targetName;
        }

        @Override // from ClickCallback
        protected void displayConfirmPopup ()
        {
            final BorderedDialog dialog = new BorderedDialog(false) {
                protected void onClosed (boolean autoClosed) {
                    setEnabled(true);
                }
            };

            dialog.setHeaderTitle(_msgs.convoComplainTitle(_targetName));

            final SmartTable content = new SmartTable(0, 10);
            content.setWidth("300px");
            content.setText(0, 0, _msgs.convoComplainTip(_targetName));
            content.setText(1, 0, _msgs.convoComplainTip2());
            content.setWidget(2, 0, _reason = MsoyUI.createTextBox("", 0, 64));
            dialog.setContents(content);
            dialog.addButton(new Button(_cmsgs.cancel(), dialog.onCancel()));
            dialog.addButton(new Button(_msgs.convoComplain(), dialog.onAction(new Command() {
                public void execute () {
                    if (_reason.getText().trim().length() == 0) {
                        // this makes the original dialog disappear for some reason
                        // TODO: nested popups?
                        MsoyUI.error(_msgs.convoInvalidReason());
                    } else {
                        takeAction(true);
                    }
                }
            })));
            dialog.show();
        }

        protected boolean callService ()
        {
            String reason = _reason.getText().trim();
            if (reason.length() == 0) {
                return false;
            }
            _mailsvc.complainConversation(_convoId, reason, this);
            return true;
        }

        protected boolean gotResult (Void result)
        {
            MsoyUI.info(_msgs.convoComplaintRegistered());
            return true;
        }

        TextBox _reason;
        String _targetName;
    }

    protected ConvosModel _model;
    protected int _convoId;

    protected static final MailMessages _msgs = GWT.create(MailMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final MailServiceAsync _mailsvc = GWT.create(MailService.class);
    protected static final WebMemberServiceAsync _membersvc = GWT.create(WebMemberService.class);
}
