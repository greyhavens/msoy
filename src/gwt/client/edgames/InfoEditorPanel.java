//
// $Id$

package client.edgames;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;


import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.edgame.gwt.EditGameService.GameData;
import com.threerings.msoy.game.gwt.GameGenre;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.item.data.all.Item;

import client.ui.LimitedTextArea;
import client.ui.MsoyUI;
import client.shell.CShell;
import client.util.ClickCallback;
import client.util.InfoCallback;

/**
 * Allows for creation and editing of basic game info.
 */
public class InfoEditorPanel extends EditorTable
{
    public InfoEditorPanel (final GameData data)
    {
        final GameInfo info = data.info;
        final TextBox name = MsoyUI.createTextBox(info.name, GameInfo.MAX_NAME_LENGTH, 40);
        addRow(_msgs.egName(), name, new Command() {
            public void execute () {
                info.name = EditorUtil.checkName(name.getText().trim());
            }
        });

        final ListBox genbox = new ListBox();
        genbox.addItem(_dmsgs.xlate("genre_" + GameGenre.HIDDEN), ""+GameGenre.HIDDEN);
        for (GameGenre genre : GameGenre.DISPLAY_GENRES) {
            genbox.addItem(_dmsgs.xlate("genre_" + genre), ""+genre);
            if (genre == info.genre) {
                genbox.setSelectedIndex(genbox.getItemCount()-1);
            }
        }
        addRow(_msgs.egGenre(), genbox, new Command() {
            public void execute () {
                GameGenre genre = GameGenre.valueOf(genbox.getValue(genbox.getSelectedIndex()));
                if (data.pubCode == null && genre != GameGenre.HIDDEN) {
                    throw new EditorUtil.ConfigException(_msgs.errMustHideUnpublished());
                }
                info.genre = genre;
            }
        });

        final LimitedTextArea descrip = new LimitedTextArea(GameInfo.MAX_DESCRIPTION_LENGTH, 40, 3);
        descrip.setText(info.description);
        addRow(_msgs.egDescrip(), descrip, new Command() {
            public void execute () {
                info.description = descrip.getText();
            }
        });

        addSpacer();

        final ListBox grbox = new ListBox();
        grbox.addItem(_msgs.egGroupNone(), ""+GameInfo.NO_GROUP);
        addRow(_msgs.egGroup(), grbox, new Command() {
            public void execute () {
                info.groupId = Integer.parseInt(grbox.getValue(grbox.getSelectedIndex()));
            }
        });
        addTip(_msgs.egGroupTip());

        final TextBox shopTag = MsoyUI.createTextBox(info.shopTag, 20, 20);
        addRow(_msgs.egShopTag(), shopTag, new Command() {
            public void execute () {
                info.shopTag = shopTag.getText().trim();
            }
        });
        addTip(_msgs.egShopTagTip());

        addSpacer();

        final MediaBox tbox = new MediaBox(
            MediaDesc.THUMBNAIL_SIZE, Item.THUMB_MEDIA, info.thumbMedia);
        addRow(_msgs.egThumb(), _msgs.egThumbTip(), tbox, new Command() {
            public void execute () {
                info.thumbMedia = EditorUtil.requireImageMedia(_msgs.egThumb(), tbox.getMedia());
            }
        });

        final MediaBox sbox = new MediaBox(
            MediaDesc.GAME_SHOT_SIZE, Item.AUX_MEDIA, info.shotMedia) {
            @Override public void mediaUploaded (String name, MediaDesc desc, int w, int h) {
                int targetW = MediaDesc.getWidth(MediaDesc.GAME_SHOT_SIZE);
                int targetH = MediaDesc.getHeight(MediaDesc.GAME_SHOT_SIZE);
                if ((w != targetW) || (h != targetH)) {
                    MsoyUI.error(_msgs.errInvalidShot(
                        String.valueOf(targetW), String.valueOf(targetH)));
                } else {
                    super.mediaUploaded(name, desc, w, h);
                }
            }
        };
        addRow(_msgs.egShot(), _msgs.egShotTip(), sbox, new Command() {
            public void execute () {
                info.shotMedia = EditorUtil.checkImageMedia(_msgs.egShot(), sbox.getMedia());
            }
        });

        final CheckBox blingBox = new CheckBox();
        blingBox.setValue(data.blingPool);
        blingBox.setEnabled(CShell.isSupport());
        addRow(_msgs.egBlingPool(), blingBox, null); // no binder

        addSpacer();

        Button save = addSaveRow();
        new ClickCallback<Void>(save) {
            protected boolean callService () {
                if (!bindChanges()) {
                    return false;
                }
                _gamesvc.updateGameInfo(info, blingBox.getValue(), this);
                return true;
            }
            protected boolean gotResult (Void result) {
                MsoyUI.info(_msgs.egGameInfoUpdated());
                return true;
            }
        };

        // load up the list of groups that can be associated with this game
        _groupsvc.getGameGroups(info.gameId, new InfoCallback<List<GroupMembership>>() {
            public void onSuccess (List<GroupMembership> groups) {
                for (GroupMembership group : groups) {
                    grbox.addItem(""+group.group, ""+group.group.getGroupId());
                    if (group.group.getGroupId() == info.groupId) {
                        grbox.setSelectedIndex(grbox.getItemCount()-1);
                    }
                }
            }
        });
    }

    protected static final GroupServiceAsync _groupsvc = GWT.create(GroupService.class);
}
