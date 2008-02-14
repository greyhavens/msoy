//
// $Id$

package client.people;

import java.util.Date;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.MemberCard;

import client.msgs.MailComposition;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.PromptPopup;

/**
 * Displays a list of members.
 */
public class MemberList extends PagedGrid
{
    public static final int PEOPLE_PER_PAGE = 20;

    public MemberList (String emptyMessage) 
    {
        super(PEOPLE_PER_PAGE, 1, MemberList.NAV_ON_BOTTOM);
        setWidth("650px");
        addStyleName("dottedGrid");
        _emptyMessage = emptyMessage;
    }

    // @Override // from PagedGrid
    protected Widget createWidget (Object item)
    {
        return new MemberWidget((MemberCard) item);
    }

    // @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return _emptyMessage;
    }

    // @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true;
    }

    protected void removeFriend (final MemberCard friend, boolean confirmed)
    {
        if (!confirmed) {
            new PromptPopup(CPeople.msgs.mlRemoveConfirm(friend.name.toString())) {
                public void onAffirmative () {
                    removeFriend(friend, true);
                }
            }.prompt();
            return;
        }

        CPeople.membersvc.removeFriend(CPeople.ident, friend.name.getMemberId(), new MsoyCallback() {
            public void onSuccess (Object result) {
                MsoyUI.info(CPeople.msgs.mlRemoved(friend.name.toString()));
                removeItem(friend);
            }
        });
    }

    protected class MemberWidget extends SmartTable
    {
        public MemberWidget (final MemberCard card) 
        {
            super("memberWidget", 0, 5);

            setWidget(0, 0, MediaUtil.createMediaView(
                          card.photo, MediaDesc.THUMBNAIL_SIZE, new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.PEOPLE, "" + card.name.getMemberId());
                }
            }), 1, "Photo");
            getFlexCellFormatter().setRowSpan(0, 0, 3);

            setWidget(0, 1, Application.createLink(card.name.toString(), Page.PEOPLE,
                                                   ""+card.name.getMemberId()), 1, "Name");

            // we'll overwrite these below if we have anything to display
            getFlexCellFormatter().setStyleName(1, 0, "Headline");
            setHTML(1, 0, "&nbsp;");
            setHTML(2, 0, "&nbsp;");

            if (card.headline != null && card.headline.length() > 0) {
                setText(1, 0, card.headline);
            }

            if (card.status instanceof MemberCard.NotOnline) {
                long lastLogon = ((MemberCard.NotOnline)card.status).lastLogon;
                setText(2, 0, CPeople.msgs.mlLastOnline(_lfmt.format(new Date(lastLogon))));

            } else if (card.status instanceof MemberCard.InGame) {
                MemberCard.InGame status = (MemberCard.InGame)card.status;
                setWidget(2, 0, createOnlineLink(CPeople.msgs.mlOnlinePlaying(status.gameName),
                                                 Page.WORLD, Args.compose("game", status.gameId)));

            } else if (card.status instanceof MemberCard.InScene) {
                MemberCard.InScene status = (MemberCard.InScene)card.status;
                setWidget(2, 0, createOnlineLink(CPeople.msgs.mlOnlineIn(status.sceneName),
                                                 Page.WORLD, "s" + status.sceneId));
            }

            SmartTable extras = new SmartTable(0, 5);
            int row = 0;
            ClickListener onClick;

// TODO: include whether this member is our friend or not in MemberCard; if they are, add a "remove
// friend" link, if they're not, add an "add friend" link

//             if (CPeople.getMemberId() == card.name.getMemberId()) {
//                 onClick = new ClickListener() {
//                     public void onClick (Widget widget) {
//                         removeFriend(card, false);
//                     }
//                 };
//                 extras.setWidget(
//                     row, 0, MsoyUI.createActionImage("/images/profile/remove.png", onClick));
//                 extras.setWidget(
//                     row++, 1, MsoyUI.createActionLabel(CPeople.msgs.mlRemove(), onClick));
//             }

            onClick = new ClickListener() {
                public void onClick (Widget widget) {
                    new MailComposition(card.name, null, null, null).show();
                }
            };
            extras.setWidget(
                row, 0, MsoyUI.createActionImage("/images/profile/sendmail.png", onClick));
            extras.setWidget(
                row++, 1, MsoyUI.createActionLabel("Send mail", onClick));

            onClick = new ClickListener() {
                public void onClick (Widget widget) {
                    Application.go(Page.WORLD, "m" + card.name.getMemberId());
                }
            };
            extras.setWidget(
                row, 0, MsoyUI.createActionImage("/images/profile/visithome.png", onClick));
            extras.setWidget(
                row++, 1, MsoyUI.createActionLabel("Visit home", onClick));

            setWidget(0, 2, extras);
            getFlexCellFormatter().setRowSpan(0, 2, getRowCount());
            getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
        }

        protected Widget createOnlineLink (String text, String page, String args)
        {
            FlowPanel panel = new FlowPanel();
            panel.add(new InlineLabel(text, false, false, true));
            Widget link = Application.createLink(CPeople.msgs.mlJoin(), page, args);
            link.addStyleName("inline");
            panel.add(link);
            return panel;
        }
    }

    protected String _emptyMessage;

    protected static SimpleDateFormat _lfmt = new SimpleDateFormat("MMM dd h:mmaa");
}
