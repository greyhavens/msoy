//
// $Id$

package client.whirleds;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.gwt.GroupExtras;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.web.data.TagHistory;

import client.item.TagDetailPanel;
import client.shell.Args;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.Pages;
import client.shell.ShellMessages;
import client.ui.LimitedTextArea;
import client.ui.MsoyUI;
import client.ui.PopupMenu;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * A popup that lets a member of sufficient rank modify a group's metadata.
 */
public class GroupEdit extends FlexTable
{
    /**
     * This constructor is for creating new Groups.
     */
    public GroupEdit ()
    {
        this(new Group(), new GroupExtras());
    }

    public GroupEdit (Group group, GroupExtras extras)
    {
        _group = group;
        _extras = extras;

        setStyleName("groupEditor");
        setCellSpacing(5);
        setCellPadding(0);

        CShell.frame.setTitle(_group.groupId == 0 ? _msgs.editCreateTitle() : group.name);

        // set up our editor contents
        _name = MsoyUI.createTextBox(_group.name, GroupName.LENGTH_MAX, 20);
        addRow(_msgs.editName(), _name);

        // if this is a blank group, set up some defaults
        if (_group.policy == 0) {
            _group.policy = Group.POLICY_PUBLIC;
            _group.forumPerms = Group.makePerms(Group.PERM_MEMBER, Group.PERM_ALL);
        }

        // make sure the group's configured policy is consistent with what's shown in the GUI
        _policy = new ListBox();
        _policy.addItem(_msgs.policyPublic());
        _policy.addItem(_msgs.policyInvite());
        _policy.addItem(_msgs.policyExclusive());
        _policy.setSelectedIndex(_group.policy - Group.POLICY_PUBLIC);
        addRow(_msgs.editPolicy(), _policy);

        _thread = new ListBox();
        _thread.addItem(_msgs.forumPermsAll());
        _thread.addItem(_msgs.forumPermsMember());
        _thread.addItem(_msgs.forumPermsManager());
        _thread.setSelectedIndex(_group.getThreadPerm() - Group.PERM_ALL);
        addRow(_msgs.editThread(), _thread);

        _post = new ListBox();
        _post.addItem(_msgs.forumPermsAll());
        _post.addItem(_msgs.forumPermsMember());
        _post.addItem(_msgs.forumPermsManager());
        _post.setSelectedIndex(_group.getPostPerm() - Group.PERM_ALL);
        addRow(_msgs.editPost(), _post);

        addRow(_msgs.editLogo(), _logo = new PhotoChoiceBox(true, null));
        _logo.setMedia(_group.getLogo());

        _blurb = MsoyUI.createTextBox(_group.blurb, Group.MAX_BLURB_LENGTH, 40);
        addRow(_msgs.editBlurb(), _blurb);

        _homepage = MsoyUI.createTextBox(_extras.homepageUrl, 255, 40);
        addRow(_msgs.editHomepage(), _homepage);

        _charter = new LimitedTextArea(Group.MAX_CHARTER_LENGTH, 60, 3);
        _charter.setText(_extras.charter);
        addRow(_msgs.editCharter(), _charter);

        _catalogType = new ListBox();
        for (int ii = 0; ii < Item.SHOP_TYPES.length; ii++) {
            _catalogType.addItem(_dmsgs.xlate("itemType" + Item.SHOP_TYPES[ii]));
            if (_extras.catalogItemType == Item.SHOP_TYPES[ii]) {
                _catalogType.setSelectedIndex(ii);
            }
        }
        addRow(_msgs.editCatalogType(), _catalogType);
        addRow(_msgs.editCatalogTag(),
            _catalogTag = MsoyUI.createTextBox(_extras.catalogTag, 24, 24));

        HorizontalPanel footer = new HorizontalPanel();
        footer.add(_submit = new Button(_cmsgs.change(), new ClickListener() {
            public void onClick (Widget sender) {
                commitEdit();
            }
        }));
        footer.add(WidgetUtil.makeShim(5, 5));
        footer.add(new Button(_cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                Link.go(Pages.WHIRLEDS, _group.groupId == 0 ? "" :
                        Args.compose("d", _group.groupId));
            }
        }));
        int frow = getRowCount();
        setWidget(frow, 1, footer);
        getFlexCellFormatter().setHorizontalAlignment(frow, 1, HasAlignment.ALIGN_RIGHT);

        // TODO integrate tags into the main form
        if (_group.groupId != 0 && _group.policy != Group.POLICY_EXCLUSIVE) {
            TagDetailPanel tags = new TagDetailPanel(new TagDetailPanel.TagService() {
                public void tag (String tag, AsyncCallback<TagHistory> cback) {
                    _groupsvc.tagGroup(_group.groupId, tag, true, cback);
                }
                public void untag (String tag, AsyncCallback<TagHistory> cback) {
                    _groupsvc.tagGroup(_group.groupId, tag, false, cback);
                }
                public void getRecentTags (AsyncCallback<List<TagHistory>> cback) {
                    _groupsvc.getRecentTags(cback);
                }
                public void getTags (AsyncCallback<List<String>> cback) {
                    _groupsvc.getTags(_group.groupId, cback);
                }
                public boolean supportFlags () {
                    return false;
                }
                public void setFlags (byte flag) {
                    // nada
                }
                public void addMenuItems (final String tag, PopupMenu menu) {
                    menu.addMenuItem(_msgs.detailTagLink(), new Command() {
                        public void execute () {
                            Link.go(Pages.WHIRLEDS, Args.compose("tag", "0", tag));
                        }
                    });
                }
            }, null, true);
            addRow("", WidgetUtil.makeShim(1, 20));
            addRow("", tags);
        }

    }

    protected void addRow (String label, Widget contents)
    {
        int row = getRowCount();
        setText(row, 0, label);
        getFlexCellFormatter().setStyleName(row, 0, "nowrapLabel");
        getFlexCellFormatter().addStyleName(row, 0, "rightLabel");
        setWidget(row, 1, contents);
    }

    protected void commitEdit ()
    {
        // extract our values
        _group.name = _name.getText().trim();
        _group.logo = _logo.getMedia();
        _group.blurb = _blurb.getText().trim();
        _group.policy = (byte)(_policy.getSelectedIndex()+Group.POLICY_PUBLIC);
        _group.forumPerms = Group.makePerms(_thread.getSelectedIndex()+Group.PERM_ALL,
                                            _post.getSelectedIndex()+Group.PERM_ALL);
        _extras.charter = _charter.getText().trim();
        _extras.homepageUrl = _homepage.getText().trim();
        _extras.catalogItemType = Item.SHOP_TYPES[_catalogType.getSelectedIndex()];
        _extras.catalogTag = _catalogTag.getText().trim();

        // check that the group name is valid
        if (_group.name.length() < GroupName.LENGTH_MIN ||
            _group.name.length() > GroupName.LENGTH_MAX ||
            !(Character.isLetter(_group.name.charAt(0)) ||
              Character.isDigit(_group.name.charAt(0)))) {
            MsoyUI.error(_msgs.errInvalidGroupName());
            return;
        }

        final MsoyCallback<Void> updateCallback = new MsoyCallback<Void>() {
            public void onSuccess (Void result) {
                Link.go(Pages.WHIRLEDS, Args.compose("d", String.valueOf(_group.groupId), "r"));
            }
        };
        final MsoyCallback<Group> createCallback = new MsoyCallback<Group>() {
            public void onSuccess (Group group) {
                Link.go(Pages.WHIRLEDS, Args.compose("d", String.valueOf(group.groupId), "r"));
            }
        };
        // check if we're trying to set the policy to exclusive on a group that has tags
        if (_group.policy == Group.POLICY_EXCLUSIVE) {
            _groupsvc.getTags(_group.groupId, new MsoyCallback<List<String>>() {
                public void onSuccess (List<String> tags) {
                    if (tags.size() > 0) {
                        MsoyUI.error(_msgs.errTagsOnExclusive());
                    } else if (_group.groupId > 0) {
                        _groupsvc.updateGroup(_group, _extras, updateCallback);
                    } else {
                        _groupsvc.createGroup(_group, _extras, createCallback);
                    }
                }
            });
        } else {
            if (_group.groupId > 0) {
                _groupsvc.updateGroup(_group, _extras, updateCallback);
            } else {
                _groupsvc.createGroup(_group, _extras, createCallback);
            }
        }
    }

    protected Group _group;
    protected GroupExtras _extras;

    protected TextBox _name, _blurb, _homepage, _catalogTag;
    protected PhotoChoiceBox _logo;
    protected ListBox _policy, _thread, _post, _catalogType;
    protected LimitedTextArea _charter;
    protected Button _submit;

    protected static final WhirledsMessages _msgs = GWT.create(WhirledsMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)
        ServiceUtil.bind(GWT.create(GroupService.class), GroupService.ENTRY_POINT);
}
