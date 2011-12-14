//
// $Id$

package client.rooms;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.ListBox;

import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.comment.data.all.CommentType;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.room.gwt.RoomDetail;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.comment.CommentsPanel;
import client.room.SceneUtil;
import client.shell.CShell;
import client.ui.CreatorLabel;
import client.ui.MsoyUI;
import client.ui.Rating.RateService;
import client.ui.Rating;
import client.ui.RoundBox;
import client.ui.StyledTabPanel;
import client.util.InfoCallback;
import client.util.Link;

/**
 * Displays information about a room, allows commenting.
 */
public class RoomDetailPanel extends SmartTable
{
    protected final class TemplateClickHandler implements ClickHandler {
        public TemplateClickHandler (boolean doMake) {
            _doMake = doMake;
        }
        public void onClick (ClickEvent event) {
            _roomsvc.makeTemplate(_detail.info.sceneId, _detail.theme.getGroupId(), _doMake,
                new InfoCallback<Void>() {
                public void onSuccess (Void result) {
                    _detail.isTemplate = _doMake;
                    updateStamps();
                }
            });
        }

        protected boolean _doMake;
    }

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
        final int snapWidth = MediaDescSize.getWidth(MediaDescSize.SNAPSHOT_FULL_SIZE);
        getFlexCellFormatter().setWidth(0, 0, snapWidth + "px");

        _bits = new FlowPanel();
        _bits.add(MsoyUI.createLabel(detail.info.name, "Title"));
        CreatorLabel by = new CreatorLabel();
        if (detail.owner instanceof MemberCard) {
            by.setMember((MemberCard)detail.owner);
        } else if (detail.owner instanceof GroupName) {
            by.setBrand((GroupName)detail.owner);
        }
        _bits.add(by);
        if (CShell.isSupport() || detail.mayManage) {
            _bits.add(_themeBit = new FlowPanel());
            updateTheme();
        }

        _bits.add(WidgetUtil.makeShim(10, 15));
        _bits.add(new Rating(
            detail.info.rating, detail.ratingCount, detail.memberRating, true, new RateService() {
                public void handleRate (byte newRating , InfoCallback<RatingResult> callback) {
                    _roomsvc.rateRoom(detail.info.sceneId, newRating, callback);
                }
            }, null));

        // maybe add the gifting option
        if ((detail.owner instanceof MemberCard) &&
                CShell.getMemberId() == ((MemberCard) detail.owner).name.getId()) {
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

        if (CShell.isSupport() || detail.mayManage) {
            _membersvc.loadManagedThemes(new InfoCallback<GroupName[]>() {
                public void onSuccess (GroupName[] result) {
                    _managedThemes = new LinkedHashSet<GroupName>(Arrays.asList(result));
                    updateStamps();
                }
            });
        }

        setWidget(0, 1, _bits);

        getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);

        StyledTabPanel tabs = new StyledTabPanel();
        CommentsPanel comments = new CommentsPanel(CommentType.ROOM, detail.info.sceneId, true);
        comments.expand();
        tabs.add(comments, _msgs.tabComments());
        addWidget(tabs, 2);
        tabs.selectTab(0);
    }

    protected void updateTheme ()
    {
        _themeBit.clear();

        if (_detail.theme != null) {
            _themeBit.add(new InlineLabel(_msgs.theme(), false, false, true));
            _themeBit.add(Link.groupView(_detail.theme.toString(), (_detail.theme).getGroupId()));
            if (_holder == null) {
                _holder = new FlowPanel();
                _holder.add(_unstampButton = MsoyUI.createTinyButton(
                    _msgs.doUnstamp(), new ClickHandler() {
                        public void onClick (ClickEvent event) {
                            _roomsvc.stampRoom(_detail.info.sceneId, _detail.theme.getGroupId(), false,
                                new InfoCallback<Void>() {
                                public void onSuccess (Void result) {
                                    _detail.theme = null;
                                    _detail.isTemplate = false;
                                    updateStamps();
                                }
                            });
                        }
                }));

                _holder.add(_templateButton = MsoyUI.createTinyButton(
                    _msgs.doTemplate(), new TemplateClickHandler(true)));
                _templateButton.setVisible(false);

                _holder.add(_unTemplateButton = MsoyUI.createTinyButton(
                    _msgs.doUntemplate(), new TemplateClickHandler(false)));
                _unTemplateButton.setVisible(false);
            }

            _themeBit.add(_holder);
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

        if (_themeBits == null) {
            _bits.add(_themeBits = new RoundBox(RoundBox.BLUE));
            _themeBits.setWidth("100%");
            _themeBits.add(_themeContents = new SmartTable());
        }
        _themeContents.setWidget(0, 0, _stampPanel = new SmartTable());
        _themeContents.setWidget(1, 0, MsoyUI.createLabel(_msgs.themeNote(), "themeNote"));

        if (_detail.theme != null) {
            if (CShell.isSupport()) {
                // support can do everything
                _unstampButton.setVisible(true);
                _unTemplateButton.setVisible(_detail.isTemplate);
                _templateButton.setVisible(!_detail.isTemplate);
                return;
            }
            boolean isThemeManager = _managedThemes.contains(_detail.theme);
            _unstampButton.setVisible(isThemeManager || _detail.mayManage);
            if (_detail.isTemplate) {
                _templateButton.setVisible(false);
                // you may untemplate a room if you manage the room OR the theme
                _unTemplateButton.setVisible(isThemeManager || _detail.mayManage);
            } else {
                _unTemplateButton.setVisible(false);
                // you may template a room if you manage the room AND the theme
                _templateButton.setVisible(isThemeManager && _detail.mayManage);
            }

        } else {
            buildStampUI();
        }
    }

    protected boolean buildStampUI ()
    {
        _stampBox = new ListBox();
        _stampBox.addItem(_msgs.noTheme());
        _stampBox.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                _stampButton.setEnabled(_stampBox.getSelectedIndex() > 0);
            }
        });
        _stampEntries = Lists.newArrayList();

        for (GroupName theme : _managedThemes) {
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
            return true;
        }
        return false;
    }

    protected RoomDetail _detail;

    protected FlowPanel _bits;
    protected FlowPanel _themeBit;
    protected RoundBox _themeBits;
    protected SmartTable _themeContents;
    protected SmartTable _stampPanel;
    protected Set<GroupName> _managedThemes;

    protected FlowPanel _holder;
    protected Button _unstampButton, _templateButton, _unTemplateButton;
    protected ListBox _stampBox;
    protected Button _stampButton;
    protected List<GroupName> _stampEntries;

    protected static final RoomsMessages _msgs = GWT.create(RoomsMessages.class);
    protected static final WebRoomServiceAsync _roomsvc = GWT.create(WebRoomService.class);
    protected static final WebMemberServiceAsync _membersvc = GWT.create(WebMemberService.class);
}
