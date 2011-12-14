//
// $Id$

package client.mail;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.DateUtil;

import com.threerings.msoy.mail.gwt.Conversation;
import com.threerings.msoy.mail.gwt.MailService;
import com.threerings.msoy.mail.gwt.MailServiceAsync;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.ClickCallback;
import client.util.Link;

/**
 * Displays the main interface for mail.
 */
public class MailPanel extends FlowPanel
{
    public MailPanel (ConvosModel model, int page)
    {
        setStyleName("mail");

        ConvosGrid grid = new ConvosGrid(MsoyUI.computeRows(FLUFF, ROW_HEIGHT, 5));
        add(grid);
        grid.setModel(model, page);
    }

    protected static class ConvosGrid extends PagedGrid<Conversation>
    {
        public ConvosGrid (int rows)
        {
            super(rows, 1, NAV_ON_TOP);
            addStyleName("Folder");
        }

        @Override // from PagedGrid
        protected Widget createWidget (Conversation convo)
        {
            // it's a PITA to extract this widget from PagedGrid, so we keep it around manually
            ConvoWidget cw = new ConvoWidget(this, convo);
            _convos.add(cw);
            return cw;
        }

        @Override // from PagedGrid
        protected String getEmptyMessage ()
        {
            return _msgs.mailNoMail();
        }

        @Override // from PagedGrid
        protected void formatCell (
            HTMLTable.CellFormatter formatter, int row, int col, Conversation convo)
        {
            super.formatCell(formatter, row, col, convo);
            if (convo.hasUnread) {
                formatter.addStyleName(row, col, "Unread");
            }
        }

        @Override // from PagedGrid
        protected void displayResults (int start, int count, List<Conversation> list)
        {
            // reset our list of displayed conversation widgets
            _convos = Lists.newArrayList();

            // create all of our conversation widgets
            super.displayResults(start, count, list);

            // create and add our custom footer
            int col = 0;
            SmartTable footer = new SmartTable("Footer", 0, 0);
            footer.setWidth("100%");
            footer.setHTML(0, col++, "&nbsp;", 1, "BottomLeft");
            CheckBox selall = new CheckBox(_cmsgs.selectAll());
            selall.addClickHandler(new ClickHandler() {
                public void onClick (ClickEvent event) {
                    boolean select = ((CheckBox)event.getSource()).getValue();
                    for (ConvoWidget cw : _convos) {
                        cw.setSelected(select);
                    }
                }
            });
            footer.setWidget(0, col++, selall, 1, "SelectAll");
            Button delsel = new Button(_msgs.mailDelSel());
            footer.setWidget(0, col++, delsel, 1);
            footer.getFlexCellFormatter().setWidth(0, col, "50%");
            footer.setHTML(0, col++, "&nbsp;");
            footer.setHTML(0, col++, "&nbsp;", 1, "BottomRight");
            add(footer);

            // wire up a callback for delete selected
            new ClickCallback<Void>(delsel) {
                @Override protected boolean callService () {
                    _convoIds = Lists.newArrayList();
                    for (ConvoWidget cw : _convos) {
                        if (cw.isSelected()) {
                            _convoIds.add(cw.getConvoId());
                        }
                    }
                    if (_convoIds.isEmpty()) {
                        return false;
                    }
                    _mailsvc.deleteConversations(_convoIds, this);
                    return true;
                }
                @Override protected boolean gotResult (Void result) {
                    for (int convoId : _convoIds) {
                        ((ConvosModel)_model).conversationDeleted(convoId);
                    }
                    displayPage(_page, true);
                    return true;
                }
                protected List<Integer> _convoIds;
            };
        }

        @Override // from PagedGrid
        protected void displayPageFromClick (int page)
        {
            Link.go(Pages.MAIL, ""+page);
        }

        @Override // from PagedGrid
        protected boolean displayNavi (int items)
        {
            return true;
        }

        @Override // from PagedGrid
        protected void addCustomControls (FlexTable controls)
        {
            super.addCustomControls(controls);

            FloatPanel customControls = new FloatPanel(null);
            customControls.add(new Button(_msgs.mailCheck(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    if (_page == 0) {
                        ((ConvosModel)_model).reset();
                        displayPage(0, true);
                    } else {
                        Link.go(Pages.MAIL, "");
                    }
                }
            }));
            Widget composeLink = Link.create(_msgs.mailCompose(), Pages.MAIL, "w");
            composeLink.addStyleName("ComposeLink");
            customControls.add(composeLink);
            controls.setWidget(0, 0, customControls);
        }

        protected List<ConvoWidget> _convos;
    }

    protected static class ConvoWidget extends SmartTable
    {
        public ConvoWidget (final ConvosGrid grid, final Conversation convo) {
            super("Convo", 0, 0);
            _convo = convo;

            setWidget(0, 0, _select = new CheckBox(), 1, "Select");
            getFlexCellFormatter().setRowSpan(0, 0, 2);

            MediaDesc photo;
            Widget name;
            if (convo.other == null) {
                photo = MemberCard.DEFAULT_PHOTO;
                name = MsoyUI.createLabel("<unknown>", null);
            } else {
                photo = convo.other.photo;
                name = Link.memberView(convo.other);
            }

            setWidget(0, 1, new ThumbBox(photo, MediaDescSize.HALF_THUMBNAIL_SIZE), 1, "Photo");
            getFlexCellFormatter().setRowSpan(0, 1, 2);

            setWidget(0, 2, name, 1, "Name");
            setText(1, 0, DateUtil.formatDateTime(convo.lastSent), 1, "Sent");

            Widget link = Link.create(
                (convo.subject.length() == 0) ? _msgs.mailNoSubject() : convo.subject,
                Pages.MAIL, "c", convo.conversationId);
            setWidget(0, 3, link, 1, "Subject");
            setText(1, 1, convo.lastSnippet, 1, "Snippet");
        }

        public int getConvoId () {
            return _convo.conversationId;
        }

        public boolean isSelected () {
            return _select.getValue();
        }

        public void setSelected (boolean selected) {
            _select.setValue(selected);
        }

        protected Conversation _convo;
        protected CheckBox _select;
    }

    protected static final MailMessages _msgs = GWT.create(MailMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final MailServiceAsync _mailsvc = GWT.create(MailService.class);

    protected static final int FLUFF = 29 * 2 + 20;
    protected static final int ROW_HEIGHT = 45;
}
