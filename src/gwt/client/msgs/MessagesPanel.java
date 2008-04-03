//
// $Id$

package client.msgs;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;

import com.threerings.msoy.fora.data.ForumMessage;
import com.threerings.msoy.fora.data.ForumThread;

import client.images.msgs.MsgsImages;
import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.util.ClickCallback;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.PromptPopup;

/**
 * Displays the messages in a particular thread.
 */
public class MessagesPanel extends PagedGrid
{
    public MessagesPanel (ThreadPanel parent, ForumModels.ThreadMessages model,
                          int page, int scrollToId)
    {
        super(MESSAGES_PER_PAGE, 1, NAV_ON_BOTTOM);
        setCellAlignment(ALIGN_LEFT, ALIGN_TOP);
        addStyleName("dottedGrid");
        setWidth("100%");
        setHeight("100%");

        _parent = parent;
        _scrollToId = scrollToId;
        _tmodel = model;

        setModel(_tmodel, page);
    }

    public void restoreThread ()
    {
        setModel(_tmodel, 0);
    }

    public void refreshDisplay ()
    {
        displayPage(_page, true);
    }

    // @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return (_model == _tmodel) ? CMsgs.mmsgs.noMessages() : CMsgs.mmsgs.noMatches();
    }

    // @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true; // we always show our navigation for consistency
    }

    // @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        super.addCustomControls(controls);

        // add a button for starting a new message that will optionally be enabled later
        _postReply = new Button(CMsgs.mmsgs.postReply(), new ClickListener() {
            public void onClick (Widget sender) {
                _parent.postReply(null, false);
            }
        });
        _postReply.setEnabled(false);
        controls.setWidget(0, 0, _postReply);

        // add a button for ignoring this thread
        _ignoreThread = new Button(CMsgs.mmsgs.ignoreThread());
        new ClickCallback(_ignoreThread, CMsgs.mmsgs.ignoreThreadConfirm()) {
            public boolean callService () {
                CMsgs.forumsvc.ignoreThread(CMsgs.ident, _parent.getThreadId(), this);
                return true;
            }
            public boolean gotResult (Object result) {
                MsoyUI.info(CMsgs.mmsgs.threadIgnored());
                return false;
            }
        };
        controls.setWidget(0, 1, _ignoreThread);

        // add a button for editing this thread's flags
        _editFlags = new Button(CMsgs.mmsgs.editFlags(), new ClickListener() {
            public void onClick (Widget sender) {
                _parent.editFlags();
            }
        });
        _editFlags.setEnabled(false);
        controls.setWidget(0, 2, _editFlags);
    }

    // @Override // from PagedGrid
    protected void displayResults (int start, int count, List list)
    {
        // if we're displaying results from our main thread model, update our ephemera; this must
        // be done before the call to super because super creates our widgets and those check our
        // ephmera to determine how they lay themselves out
        if (_model == _tmodel) {
            _parent.gotThread(_tmodel.getThread());
            _postReply.setEnabled(_tmodel.canPostReply() && !_tmodel.getThread().isLocked());
            _editFlags.setEnabled(_tmodel.isManager());
        }

        super.displayResults(start, count, list);

        if (_scrollToPanel != null) {
            DeferredCommand.addCommand(new Command() {
                public void execute () {
                    Frame.ensureVisible(_scrollToPanel);
                    _scrollToPanel = null;
                }
            });
        }
    }

    // @Override // from PagedGrid
    protected Widget createWidget (Object item)
    {
        ForumMessage msg = (ForumMessage)item;
        ThreadMessagePanel panel = new ThreadMessagePanel(_tmodel.getThread(), msg);
        if (msg.messageId == _scrollToId) {
            _scrollToId = 0;
            _scrollToPanel = panel;
        } else if (_scrollToPanel == null) {
            _scrollToPanel = panel;
        }
        return panel;
    }

    protected void replyPosted (ForumMessage message)
    {
        MsoyUI.info(CMsgs.mmsgs.msgReplyPosted());
        _tmodel.appendItem(message);
        if (_model == _tmodel) {
            refreshDisplay();
        }
        // TODO: what to do if you post a reply while searching?
    }

    protected Command deletePost (final ForumMessage message)
    {
        return new Command() {
            public void execute () {
                // TODO: if forum admin, make them send a mail to the poster explaining why their
                // post was deleted?
                CMsgs.forumsvc.deleteMessage(CMsgs.ident, message.messageId, new MsoyCallback() {
                    public void onSuccess (Object result) {
                        removeItem(message);
                        MsoyUI.info(CMsgs.mmsgs.msgPostDeleted());
                    }
                });
            }
        };
    }

    protected static Widget makeInfoImage (
        AbstractImagePrototype iproto, String tip, ClickListener onClick)
    {
        Widget image = MsoyUI.makeActionImage(iproto.createImage(), tip, onClick);
        image.addStyleName("ActionIcon");
        return image;
    }

    protected static InlineLabel makeInfoLabel (String text, ClickListener listener)
    {
        InlineLabel label = new InlineLabel(text, false, true, false);
        label.addClickListener(listener);
        label.addStyleName("Posted");
        label.addStyleName("actionLabel");
        return label;
    }

    protected class ThreadMessagePanel extends SimpleMessagePanel
    {
        public ThreadMessagePanel (ForumThread thread, ForumMessage message)
        {
            _thread = thread;
            setMessage(message);
        }

        public void setMessage (ForumMessage message)
        {
            _message = message;
            super.setMessage(message);
        }

        // @Override // from MessagePanel
        protected void addInfo (FlowPanel info)
        {
            super.addInfo(info);

            if (CMsgs.getMemberId() != 0 &&
                CMsgs.getMemberId() != _message.poster.name.getMemberId()) {
                String args = Args.compose("w", "m", ""+_message.poster.name.getMemberId());
                info.add(makeInfoLabel(CMsgs.mmsgs.inlineMail(),
                                       Application.createLinkListener(Page.MAIL, args)));
            }

            if (_postReply.isEnabled()) {
                info.add(makeInfoImage(_images.reply_post(),
                                                CMsgs.mmsgs.inlineReply(), new ClickListener() {
                    public void onClick (Widget sender) {
                        _parent.postReply(_message, false);
                    }
                }));
                info.add(makeInfoImage(_images.reply_post_quote(),
                                                CMsgs.mmsgs.inlineQReply(), new ClickListener() {
                    public void onClick (Widget sender) {
                        _parent.postReply(_message, true);
                    }
                }));
            }

            if (CMsgs.getMemberId() == _message.poster.name.getMemberId()) {
                info.add(makeInfoImage(_images.edit_post(),
                                                CMsgs.mmsgs.inlineEdit(), new ClickListener() {
                    public void onClick (Widget sender) {
                        _parent.editPost(_message, new MsoyCallback() {
                            public void onSuccess (Object result) {
                                setMessage((ForumMessage)result);
                            }
                        });
                    }
                }));
            }

            // TODO: if whirled manager, also allow forum moderation
            if (CMsgs.getMemberId() == _message.poster.name.getMemberId() || CMsgs.isAdmin()) {
                info.add(makeInfoImage(_images.delete_post(),
                                                CMsgs.mmsgs.inlineDelete(),
                                                new PromptPopup(CMsgs.mmsgs.confirmDelete(),
                                                                deletePost(_message))));
            }

            if (_message.issueId > 0) {
                ClickListener viewClick = Application.createLinkListener(
                    Page.WHIRLEDS, Args.compose("i", _message.issueId));
                info.add(makeInfoImage(_images.view_issue(),
                                                CMsgs.mmsgs.inlineIssue(), viewClick));

            } else if (CMsgs.isAdmin()) {
                ClickListener newClick = new ClickListener() {
                    public void onClick (Widget sender) {
                        _parent.newIssue(_message);
                    }
                };
                info.add(makeInfoImage(_images.new_issue(),
                                                CMsgs.mmsgs.inlineNewIssue(), newClick));
                info.add(makeInfoImage(_images.assign_issue(), CMsgs.mmsgs.inlineAssignIssue(),
                                       Application.createLinkListener(
                                           Page.WHIRLEDS, Args.compose(
                                               "assign", ""+_message.messageId, ""+_page))));
            }
        }

        // @Override // from MessagePanel
        protected String getIconPath ()
        {
            return "/images/msgs/" +
                ((_message.messageId > _thread.lastReadPostId) ? "unread" : "read") + ".png";
        }

        protected ForumThread _thread;
        protected ForumMessage _message;
    }

    /** The thread panel in which we're hosted. */
    protected ThreadPanel _parent;

    /** Our thread messages model. We keep this around because we may temporarily replace it with a
     * search model if the user does a search. */
    protected ForumModels.ThreadMessages _tmodel;

    /** A message to scroll into view when we first receive our messages. */
    protected int _scrollToId;

    /** The panel to which we want to scroll once our page is laid out. */
    protected ThreadMessagePanel _scrollToPanel;

    /** A button for posting a reply message. */
    protected Button _postReply;

    /** A button for ignoring this thread. */
    protected Button _ignoreThread;

    /** A button for editing this thread's flags. */
    protected Button _editFlags;

    /** Our action icon images. */
    protected static MsgsImages _images = (MsgsImages)GWT.create(MsgsImages.class);

    protected static final int MESSAGES_PER_PAGE = 10;
}
