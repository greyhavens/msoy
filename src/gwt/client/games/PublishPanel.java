//
// $Id$

package client.games;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameCode;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;

import client.ui.MsoyUI;
import client.util.ClickCallback;

/**
 * Displays an interface for publishing development code to production code.
 */
public class PublishPanel extends SmartTable
{
    public PublishPanel (final GameService.GameData data)
    {
        super("publish", 0, 10);

        int row = 0;
        setText(row, 0, _msgs.publishIntro(), 3, null);

        setText(++row, 1, _msgs.publishInDev(), 1, "Header");
        setText(row, 2, _msgs.publishPub(), 1, "Header");

        _dataRow = ++row;
        row = updateData(data);

        setWidget(++row, 1, _publish);
        new ClickCallback<Void>(_publish) {
            protected boolean callService () {
                _gamesvc.publishGameCode(data.info.gameId, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                data.pubCode = data.devCode;
                MsoyUI.info(_msgs.publishPublished());
                updateData(data);
                return false;
            }
        };
    }

    protected int updateData (GameService.GameData data)
    {
        GameCode pubCode = (data.pubCode == null) ? new GameCode() : data.pubCode;
        int row = _dataRow;

        setText(row, 0, _msgs.publishLastUp(), 1, "rightLabel");
        setText(row, 1, DateUtil.formatDateTime(new Date(data.devCode.lastUpdated)));
        if (pubCode.lastUpdated > 0) {
            setText(row, 2, DateUtil.formatDateTime(new Date(pubCode.lastUpdated)));
        }

        setText(++row, 0, _msgs.publishClientCode(), 1, "rightLabel");
        setText(row, 1, toString(data.devCode.clientMedia));
        setText(row, 2, toString(pubCode.clientMedia));

        setText(++row, 0, _msgs.publishServerCode(), 1, "rightLabel");
        setText(row, 1, toString(data.devCode.serverMedia));
        setText(row, 2, toString(pubCode.serverMedia));

        _publish.setEnabled(pubCode.lastUpdated < data.devCode.lastUpdated);
        return row;
    }

    protected static String toString (MediaDesc desc)
    {
        return (desc == null) ? "" :
            MediaDesc.hashToString(desc.hash).substring(0, 30) + ".." +
            MediaDesc.mimeTypeToSuffix(desc.mimeType);
    }

    protected int _dataRow;
    protected Button _publish = new Button(_msgs.publishPublish());

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
