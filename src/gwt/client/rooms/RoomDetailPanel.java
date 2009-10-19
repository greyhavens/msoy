//
// $Id$

package client.rooms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.ListBox;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.comment.gwt.Comment;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.RatingResult;

import com.threerings.msoy.room.gwt.RoomDetail;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;

import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.comment.CommentsPanel;
import client.room.SceneUtil;
import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.Rating;
import client.ui.RoundBox;
import client.ui.StyledTabPanel;
import client.util.Link;
import client.util.InfoCallback;

/**
 * Displays information about a room, allows commenting.
 */
public class RoomDetailPanel extends SmartTable
{
    public RoomDetailPanel (int sceneId)
    {
        super("roomDetailPanel", 0, 10);

        _roomsvc.loadRoomDetail(sceneId, new InfoCallback<RoomDetail>() {
            public void onSuccess (RoomDetail detail) {
                init(detail);
            }
        });
    }

    protected void init (final RoomDetail detail)
    {
        _detail = detail;

        if (detail == null) {
            setText(0, 0, "That room does not exist.");
            return;
        }

        CShell.frame.setTitle(detail.info.name);

        setWidget(0, 0, SceneUtil.createSceneView(detail.info.sceneId, detail.snapshot));
        final int snapWidth = MediaDesc.getWidth(MediaDesc.SNAPSHOT_FULL_SIZE);
        getFlexCellFormatter().setWidth(0, 0, snapWidth + "px");

        _bits = new FlowPanel();
        _bits.add(MsoyUI.createLabel(detail.info.name, "Title"));
        _bits.add(new InlineLabel(_msgs.owner(), false, false, true));
        if (detail.owner instanceof MemberName) {
            MemberName name = (MemberName)detail.owner;
            _bits.add(Link.memberView(name.toString(), name.getMemberId()));
        } else if (detail.owner instanceof GroupName) {
            GroupName name = (GroupName)detail.owner;
            _bits.add(Link.groupView(name.toString(), name.getGroupId()));
        }
        _bits.add(_themeBit = new FlowPanel());
        updateTheme();

        _bits.add(WidgetUtil.makeShim(10, 15));
        _bits.add(new Rating(detail.info.rating, detail.ratingCount, detail.memberRating, true) {
            @Override
            protected void handleRate (byte newRating , InfoCallback<RatingResult> callback) {
                _roomsvc.rateRoom(detail.info.sceneId, newRating, callback);
            }
        });

        // maybe add the gifting option
        if ((detail.owner instanceof MemberName) &&
                CShell.getMemberId() == ((MemberName) detail.owner).getMemberId()) {
            _bits.add(MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.gift(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    _roomsvc.canGiftRoom(detail.info.sceneId, new InfoCallback<Void>() {
                        public void onSuccess (Void nada) {
                            Link.go(Pages.MAIL, "w", "r", detail.info.sceneId);
                        }
                    });
                }
            }));
            // TODO: verbiage about gifting: all items, bla bla bla?
        }

        if (DeploymentConfig.devDeployment && !CShell.isGuest()) {
            CShell.log("Loading managed themes");
            _membersvc.loadManagedThemes(new InfoCallback<GroupName[]>() {
                public void onSuccess (GroupName[] result) {
                    _managedThemes = new LinkedHashSet<GroupName>(Arrays.asList(result));
                    CShell.log("Sorted managed themes: " + _managedThemes);
                    updateStamps();
                }
            });
        }

        setWidget(0, 1, _bits);

        getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);

        StyledTabPanel tabs = new StyledTabPanel();
        tabs.add(new CommentsPanel(Comment.TYPE_ROOM, detail.info.sceneId, true),
                 _msgs.tabComments());
        addWidget(tabs, 2);
        tabs.selectTab(0);
    }

    protected void updateTheme ()
    {
        _themeBit.clear();

        if (_detail.theme != null) {
            _themeBit.add(new InlineLabel(_msgs.theme(), false, false, true));
            _themeBit.add(Link.groupView(_detail.theme.toString(), (_detail.theme).getGroupId()));
            if (_unstampHolder == null) {
                _unstampHolder = new FlowPanel();
                _unstampHolder.add(MsoyUI.createTinyButton(_msgs.doUnstamp(), new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        _roomsvc.stampRoom(_detail.info.sceneId, _detail.theme.getGroupId(), false,
                            new InfoCallback<Void>() {
                            public void onSuccess (Void result) {
                                _detail.theme = null;
                                updateStamps();
                            }
                        });
                    }
                }));
            }
            _themeBit.add(_unstampHolder);
        }
    }

    protected void updateStamps ()
    {
        if (_stampPanel != null) {
            _stampPanel.clear();
        }
        updateTheme();

        if (_managedThemes == null || _managedThemes.isEmpty()) {
            return;
        }

        if (_detail.theme == null) {
            if (_themeBits == null) {
                CShell.log("Initializing theme bits");
                _bits.add(_themeBits = new RoundBox(RoundBox.BLUE));
                _themeBits.setWidth("100%");
                _themeBits.add(_themeContents = new SmartTable());
            }
            _themeContents.setWidget(0, 0, _stampPanel = new SmartTable());

            CShell.log("Building stamp UI");
            buildStampUI();
        }
    }

    protected void buildStampUI ()
    {
        _stampBox = new ListBox();
        _stampBox.addItem(_msgs.noTheme());
        _stampBox.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                _stampButton.setEnabled(_stampBox.getSelectedIndex() > 0);
            }
        });
        _stampEntries = new ArrayList<GroupName>();

        for (GroupName theme : _managedThemes) {
            CShell.log("Adding to drop down: " + theme);
            _stampBox.addItem(theme.toString());
            _stampEntries.add(theme);
        }

        if (_stampBox.getItemCount() > 1) {
            _stampPanel.setWidget(0, 0, _stampBox, 1);
            _stampButton = MsoyUI.createTinyButton(_msgs.doStamp(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    int ix = _stampBox.getSelectedIndex();
                    if (ix == 0) {
                        Popups.errorNear(_msgs.nothingToStamp(), _stampButton);
                        return;
                    }
                    final GroupName theme = _stampEntries.get(ix-1);
                    _roomsvc.stampRoom(_detail.info.sceneId, theme.getGroupId(), true,
                        new InfoCallback<Void>() {
                            public void onSuccess (Void result) {
                                _detail.theme = theme;
                                updateStamps();
                            }
                        });
                }
            });
            _stampButton.setEnabled(false);
            _stampPanel.setWidget(0, 2, _stampButton);
        }
    }

    protected RoomDetail _detail;

    protected FlowPanel _bits;
    protected FlowPanel _themeBit;
    protected RoundBox _themeBits;
    protected SmartTable _themeContents;
    protected SmartTable _stampPanel;
    protected Set<GroupName> _managedThemes;

    protected FlowPanel _unstampHolder;
    protected ListBox _stampBox;
    protected Button _stampButton;
    protected List<GroupName> _stampEntries;

    protected static final RoomsMessages _msgs = GWT.create(RoomsMessages.class);
    protected static final WebRoomServiceAsync _roomsvc = GWT.create(WebRoomService.class);
    protected static final WebMemberServiceAsync _membersvc = GWT.create(WebMemberService.class);
}
