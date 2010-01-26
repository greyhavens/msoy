//
// $Id$

package com.threerings.msoy.admin.server;

import com.google.inject.Inject;

import com.threerings.presents.peer.data.ClientInfo;

import com.threerings.pulse.server.AbstractPulseManager;
import com.threerings.pulse.server.persist.PulseRecord;

import com.threerings.msoy.admin.server.persist.MsoyPulseRecord;
import com.threerings.msoy.data.MsoyAuthName;
import com.threerings.msoy.game.data.GameAuthName;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;

/**
 * Records a pulse of what's going on on the Whirled server.
 */
public class MsoyPulseRecorder implements AbstractPulseManager.Recorder
{
    // from interface PulseManager.Recorder
    public Class<? extends PulseRecord> getRecordClass ()
    {
        return MsoyPulseRecord.class;
    }

    // from interface PulseManager.Recorder
    public PulseRecord takePulse (long now)
    {
        MsoyPulseRecord record = new MsoyPulseRecord();

        MsoyNodeObject nodeobj = (MsoyNodeObject)_peerMan.getNodeObject();
        record.rooms = nodeobj.hostedScenes.size();
        record.games = nodeobj.hostedGames.size();
        record.channels = nodeobj.hostedChannels.size();
        record.parties = nodeobj.hostedParties.size();
        for (ClientInfo cinfo : nodeobj.clients) {
            if (cinfo.username instanceof MsoyAuthName) {
                record.members++;
            } else if (cinfo.username instanceof GameAuthName) {
                record.gamers++;
            }
        }
        record.partiers = nodeobj.memberParties.size();

        return record;
    }

    @Inject protected MsoyPeerManager _peerMan;
}
