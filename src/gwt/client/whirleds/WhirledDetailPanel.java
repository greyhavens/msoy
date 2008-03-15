//
// $Id$

package client.whirleds;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.group.data.GroupDetail;
import com.threerings.msoy.group.data.GroupExtras;
import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.CatalogQuery;

import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.shell.WorldClient;
import client.shop.CShop;
import client.util.CreatorLabel;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.PopupMenu;
import client.util.PrettyTextPanel;
import client.util.PromptPopup;
import client.util.RoundBox;
import client.util.StyledTabPanel;
import client.util.TagDetailPanel;

/**
 * Displays the details of a Whirled.
 */
public class WhirledDetailPanel extends VerticalPanel
{
    public WhirledDetailPanel ()
    {
        setStyleName("whirledDetail");
        setSpacing(10);
    }

    /**
     * Configures this view to display the specified group.
     */
    public void setGroup (int groupId, boolean refresh)
    {
        if (_group == null || _group.groupId != groupId || refresh) {
            loadGroup(groupId);
        }
    }

    /**
     * Returns the currently loaded group.
     */
    public Group getGroup ()
    {
        return _group;
    }

    /**
     * Returns the currently loaded group extras.
     */
    public GroupExtras getGroupExtras ()
    {
        return _extras;
    }

    /**
     * Fetches the details of the group from the backend and trigger a UI rebuild.
     */
    protected void loadGroup (int groupId)
    {
        CWhirleds.groupsvc.getGroupDetail(CWhirleds.ident, groupId, new MsoyCallback() {
            public void onSuccess (Object result) {
                setGroupDetail((GroupDetail) result);
            }
        });
    }

    /**
     * Configures this view with its group detail and sets up the UI from scratch.
     */
    protected void setGroupDetail (GroupDetail detail)
    {
        clear();

        _detail = detail;
        if (_detail == null) {
            _group = null;
            add(MsoyUI.createLabel("That Whirled could not be found.", "infoLabel"));
            return;
        }

        Frame.setTitle(_detail.group.name);
        _group = _detail.group;
        _extras = _detail.extras;

        HorizontalPanel main = new HorizontalPanel();
        main.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
        add(main);

        FlowPanel window = new FlowPanel();
        SimplePanel panel = new SimplePanel();
        panel.setStyleName("Whirled");
        window.add(panel);
        window.add(MsoyUI.createLabel(CWhirleds.msgs.detailEnter(), "Enter"));

        if (_group.policy != Group.POLICY_EXCLUSIVE) {
            window.add(WidgetUtil.makeShim(10, 10));
            window.add(new TagDetailPanel(new TagDetailPanel.TagService() {
                public void tag (String tag, AsyncCallback cback) {
                    CWhirleds.groupsvc.tagGroup(CWhirleds.ident, _group.groupId, tag, true, cback);
                }
                public void untag (String tag, AsyncCallback cback) {
                    CWhirleds.groupsvc.tagGroup(CWhirleds.ident, _group.groupId, tag, false, cback);
                }
                public void getRecentTags (AsyncCallback cback) {
                    CWhirleds.groupsvc.getRecentTags(CWhirleds.ident, cback);
                }
                public void getTags (AsyncCallback cback) {
                    CWhirleds.groupsvc.getTags(CWhirleds.ident, _group.groupId, cback);
                }
                public boolean supportFlags () {
                    return false;
                }
                public void setFlags (byte flag) {
                    // nada
                }
                public void addMenuItems (final String tag, PopupMenu menu) {
                    menu.addMenuItem(CWhirleds.msgs.detailTagLink(), new Command() {
                        public void execute () {
                            Application.go(Page.WHIRLEDS, Args.compose("tag", "0", tag));
                        }
                    });
                }
            }, _detail.myRank == GroupMembership.RANK_MANAGER));
        }

        main.add(window);
        main.add(WidgetUtil.makeShim(10, 10));
        WorldClient.displayFeaturedPlace(_group.homeSceneId, panel);

        RoundBox bits = new RoundBox(RoundBox.BLUE);
        bits.addStyleName("Bits");
        bits.add(MsoyUI.createLabel(_group.name, "Name"));

        FlowPanel established = new FlowPanel();
        established.setStyleName("Established");
        established.add(new InlineLabel(CWhirleds.msgs.groupEst(_efmt.format(_group.creationDate)),
                                        false, false, true));
        CreatorLabel creator = new CreatorLabel(_detail.creator);
        creator.addStyleName("inline");
        established.add(creator);
        bits.add(established);

        if (_group.blurb != null) {
            bits.add(WidgetUtil.makeShim(10, 10));
            RoundBox blurb = new RoundBox(RoundBox.WHITE);
            blurb.setWidth("100%");
            blurb.add(MsoyUI.createLabel(_group.blurb, null));
            bits.add(blurb);
        }

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.add(MsoyUI.createButton(MsoyUI.SHORT_THIN, CWhirleds.msgs.detailEnter(),
                                        Application.createLinkListener(
                                            Page.WORLD, "s"+_group.homeSceneId)));
        buttons.add(WidgetUtil.makeShim(10, 10));
        buttons.add(MsoyUI.createButton(MsoyUI.MEDIUM_THIN, CWhirleds.msgs.detailForums(),
                                        Application.createLinkListener(
                                            Page.WHIRLEDS, Args.compose("f", _group.groupId))));
        bits.add(WidgetUtil.makeShim(10, 10));
        bits.add(buttons);

        FlowPanel extras = new FlowPanel();
        if (_detail.myRank == GroupMembership.RANK_MANAGER) {
            extras.add(Application.createLink(CWhirleds.msgs.detailEdit(), Page.WHIRLEDS,
                                              Args.compose("edit", _group.groupId)));
        }
        if (_detail.myRank == GroupMembership.RANK_NON_MEMBER) {
            if (_group.policy == Group.POLICY_PUBLIC && CWhirleds.getMemberId() > 0) {
                extras.add(MsoyUI.createActionLabel(
                               CWhirleds.msgs.detailJoin(), new PromptPopup(
                                   CWhirleds.msgs.detailJoinPrompt(), joinGroup()).setContext(
                                       CWhirleds.msgs.detailJoinContext(_group.name))));
            }
        } else {
            extras.add(MsoyUI.createActionLabel(
                           CWhirleds.msgs.detailLeave(), new PromptPopup(
                               CWhirleds.msgs.detailLeavePrompt(_group.name),
                               removeMember(CWhirleds.getMemberId()))));
        }
        if (extras.getWidgetCount() > 0) {
            bits.add(WidgetUtil.makeShim(10, 10));
            bits.add(MsoyUI.createLabel(CWhirleds.msgs.detailExtras(), null));
            bits.add(extras);
        }

        VerticalPanel bitsColumn = new VerticalPanel();
        bitsColumn.add(bits);
        if (_extras.catalogTag != null && !_extras.catalogTag.equals("")) {
            String label = CWhirleds.msgs.detailBrowseShop(
                CWhirleds.dmsgs.getString("pItemType" + _extras.catalogItemType));
            CatalogQuery query = new CatalogQuery();
            query.itemType = _extras.catalogItemType;
            query.tag = _extras.catalogTag;
            Button browseButton = new Button(
                label, Application.createLinkListener(Page.SHOP, CShop.composeArgs(query, 0)));
            bitsColumn.add(WidgetUtil.makeShim(10, 10));
            bitsColumn.add(browseButton);
        }
        main.add(bitsColumn);

        add(_tabs = new StyledTabPanel());

        String charter = (_extras.charter == null) ?
            CWhirleds.msgs.detailNoCharter() : _extras.charter;
        _tabs.add(new PrettyTextPanel(charter), CWhirleds.msgs.detailTabCharter());
        _tabs.selectTab(0);

        _tabs.add(new WhirledMembersPanel(_detail), CWhirleds.msgs.detailTabMembers());

        if (_detail.myRank == GroupMembership.RANK_MANAGER) {
            _tabs.add(new WhirledRoomsPanel(_detail), CWhirleds.msgs.detailTabRooms());
        }

//         setBackgroundImage(
//             _extras.background, _extras.backgroundControl == GroupExtras.BACKGROUND_TILED);
    }

    protected String getPolicyName (int policy)
    {
        String policyName;
        switch(policy) {
        case Group.POLICY_PUBLIC: policyName = CWhirleds.msgs.policyPublic(); break;
        case Group.POLICY_INVITE_ONLY: policyName = CWhirleds.msgs.policyInvite(); break;
        case Group.POLICY_EXCLUSIVE: policyName = CWhirleds.msgs.policyExclusive(); break;
        default: policyName = CWhirleds.msgs.errUnknownPolicy(Integer.toString(policy));
        }
        return policyName;
    }

    protected void setBackgroundImage (MediaDesc background, boolean repeat)
    {
        if (background == null) {
            DOM.setStyleAttribute(getElement(), "background", "none");
        } else {
            DOM.setStyleAttribute(
                getElement(), "backgroundImage", "url(" + background.getMediaPath() + ")");
            DOM.setStyleAttribute(
                getElement(), "backgroundRepeat", repeat ? "repeat" : "no-repeat");
        }
    }

    protected Command removeMember (final int memberId)
    {
        return new Command() {
            public void execute () {
                CWhirleds.groupsvc.leaveGroup(CWhirleds.ident, _group.groupId, memberId, refresh());
            }
        };
    }

    protected Command joinGroup ()
    {
        return new Command() {
            public void execute () {
                CWhirleds.groupsvc.joinGroup(CWhirleds.ident, _group.groupId, refresh());
            }
        };
    }

    protected MsoyCallback refresh ()
    {
        return new MsoyCallback() {
            public void onSuccess (Object result) {
                loadGroup(_group.groupId);
            }
        };
    }

    protected Group _group;
    protected GroupDetail _detail;
    protected GroupExtras _extras;
    protected StyledTabPanel _tabs;

    protected static SimpleDateFormat _efmt = new SimpleDateFormat("MMM dd, yyyy");
}
