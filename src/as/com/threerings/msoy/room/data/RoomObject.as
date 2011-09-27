// GENERATED PREAMBLE START
package com.threerings.msoy.room.data {

import org.osflash.signals.Signal;

import com.threerings.io.ObjectInputStream;

import com.threerings.util.Iterator;
import com.threerings.util.Name;

import com.threerings.presents.dobj.DSet;

import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.msoy.data.PrimaryPlace;
import com.threerings.msoy.data.all.RoomName;
import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.party.data.PartyLeader;
import com.threerings.msoy.party.data.PartyPlaceObject;
import com.threerings.msoy.party.data.PartySummary;
import com.threerings.msoy.room.data.Deejay;
import com.threerings.msoy.room.data.EntityControl;
import com.threerings.msoy.room.data.EntityMemories;
import com.threerings.msoy.room.data.RecentTrack;
import com.threerings.msoy.room.data.RoomMarshaller;
import com.threerings.msoy.room.data.RoomPropertiesEntry;
import com.threerings.msoy.room.data.Track;

// GENERATED PREAMBLE END
// GENERATED CLASSDECL START
public class RoomObject extends SpotSceneObject
    implements PrimaryPlace, PartyPlaceObject
{
// GENERATED CLASSDECL END

    // Force linkage
    MemoryChangedEvent;

    public static const TRACK_SKIPPED_MESSAGE :String = "track_skipped";

// GENERATED STREAMING START
    public var name :String;

    public var owner :Name;

    public var accessControl :int;

    public var roomService :RoomMarshaller;

    public var memories :DSet; /* of */ EntityMemories;

    public var controllers :DSet; /* of */ EntityControl;

    public var propertySpaces :DSet; /* of */ RoomPropertiesEntry;

    public var parties :DSet; /* of */ PartySummary;

    public var partyLeaders :DSet; /* of */ PartyLeader;

    public var playlist :DSet; /* of */ Audio;

    public var currentSongId :int;

    public var djs :DSet; /* of */ Deejay;

    public var currentDj :int;

    public var track :Track;

    public var trackRating :int;

    public var recentTracks :DSet; /* of */ RecentTrack;

    public var playCount :int;

    public var nameChanged :Signal = new Signal(String, String);
    public var ownerChanged :Signal = new Signal(Name, Name);
    public var accessControlChanged :Signal = new Signal(int, int);
    public var roomServiceChanged :Signal = new Signal(RoomMarshaller, RoomMarshaller);
    public var memoriesChanged :Signal = new Signal(DSet, DSet);
    public var memoriesEntryAdded :Signal = new Signal(EntityMemories);
    public var memoriesEntryRemoved :Signal = new Signal(EntityMemories);
    public var memoriesEntryUpdated :Signal = new Signal(EntityMemories, EntityMemories);
    public var controllersChanged :Signal = new Signal(DSet, DSet);
    public var controllersEntryAdded :Signal = new Signal(EntityControl);
    public var controllersEntryRemoved :Signal = new Signal(EntityControl);
    public var controllersEntryUpdated :Signal = new Signal(EntityControl, EntityControl);
    public var propertySpacesChanged :Signal = new Signal(DSet, DSet);
    public var propertySpacesEntryAdded :Signal = new Signal(RoomPropertiesEntry);
    public var propertySpacesEntryRemoved :Signal = new Signal(RoomPropertiesEntry);
    public var propertySpacesEntryUpdated :Signal = new Signal(RoomPropertiesEntry, RoomPropertiesEntry);
    public var partiesChanged :Signal = new Signal(DSet, DSet);
    public var partiesEntryAdded :Signal = new Signal(PartySummary);
    public var partiesEntryRemoved :Signal = new Signal(PartySummary);
    public var partiesEntryUpdated :Signal = new Signal(PartySummary, PartySummary);
    public var partyLeadersChanged :Signal = new Signal(DSet, DSet);
    public var partyLeadersEntryAdded :Signal = new Signal(PartyLeader);
    public var partyLeadersEntryRemoved :Signal = new Signal(PartyLeader);
    public var partyLeadersEntryUpdated :Signal = new Signal(PartyLeader, PartyLeader);
    public var playlistChanged :Signal = new Signal(DSet, DSet);
    public var playlistEntryAdded :Signal = new Signal(Audio);
    public var playlistEntryRemoved :Signal = new Signal(Audio);
    public var playlistEntryUpdated :Signal = new Signal(Audio, Audio);
    public var currentSongIdChanged :Signal = new Signal(int, int);
    public var djsChanged :Signal = new Signal(DSet, DSet);
    public var djsEntryAdded :Signal = new Signal(Deejay);
    public var djsEntryRemoved :Signal = new Signal(Deejay);
    public var djsEntryUpdated :Signal = new Signal(Deejay, Deejay);
    public var currentDjChanged :Signal = new Signal(int, int);
    public var trackChanged :Signal = new Signal(Track, Track);
    public var trackRatingChanged :Signal = new Signal(int, int);
    public var recentTracksChanged :Signal = new Signal(DSet, DSet);
    public var recentTracksEntryAdded :Signal = new Signal(RecentTrack);
    public var recentTracksEntryRemoved :Signal = new Signal(RecentTrack);
    public var recentTracksEntryUpdated :Signal = new Signal(RecentTrack, RecentTrack);
    public var playCountChanged :Signal = new Signal(int, int);

    public static const NAME :String = "name";
    public static const OWNER :String = "owner";
    public static const ACCESS_CONTROL :String = "accessControl";
    public static const ROOM_SERVICE :String = "roomService";
    public static const MEMORIES :String = "memories";
    public static const CONTROLLERS :String = "controllers";
    public static const PROPERTY_SPACES :String = "propertySpaces";
    public static const PARTIES :String = "parties";
    public static const PARTY_LEADERS :String = "partyLeaders";
    public static const PLAYLIST :String = "playlist";
    public static const CURRENT_SONG_ID :String = "currentSongId";
    public static const DJS :String = "djs";
    public static const CURRENT_DJ :String = "currentDj";
    public static const TRACK :String = "track";
    public static const TRACK_RATING :String = "trackRating";
    public static const RECENT_TRACKS :String = "recentTracks";
    public static const PLAY_COUNT :String = "playCount";

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        name = ins.readField(String);
        owner = ins.readObject(Name);
        accessControl = ins.readByte();
        roomService = ins.readObject(RoomMarshaller);
        memories = ins.readObject(DSet);
        controllers = ins.readObject(DSet);
        propertySpaces = ins.readObject(DSet);
        parties = ins.readObject(DSet);
        partyLeaders = ins.readObject(DSet);
        playlist = ins.readObject(DSet);
        currentSongId = ins.readInt();
        djs = ins.readObject(DSet);
        currentDj = ins.readInt();
        track = ins.readObject(Track);
        trackRating = ins.readInt();
        recentTracks = ins.readObject(DSet);
        playCount = ins.readInt();
    }

    public function RoomObject ()
    {
        new Signaller(this);
    }
// GENERATED STREAMING END

    // from PrimaryPlace
    public function getName () :Name
    {
        // TODO: return the actual scene id for this room
        // For now, we can fake it as a non-zero scene id and that turns out to
        // be good enough for current chat purposes
        return new RoomName(name, 1);
    }

    // from PartyPlaceObject
    public function getParties () :DSet
    {
        return parties;
    }

    // from PartyPlaceObject
    public function getOccupants () :DSet
    {
        return occupantInfo;
    }

    // from PartyPlaceObject
    public function getPartyLeaders () :DSet
    {
        return partyLeaders;
    }

    /**
     * Finds the info of an occupant who is also a member and has a given member id. Performs the
     * same function as <code>getOccupantInfo(new MemberName("", memberId))</code>, but is more
     * convenient to call and performs better.
     */
    public function getMemberInfo (memberId :int) :MemberInfo
    {
        var itr :Iterator = occupantInfo.iterator();
        while (itr.hasNext()) {
            var minfo :MemberInfo = (itr.next() as MemberInfo);
            if (minfo != null && minfo.getMemberId() == memberId) {
                return minfo;
            }
        }
        return null;
    }

    public function inDjMode () :Boolean
    {
        return !djs.isEmpty();
    }

// GENERATED CLASSFINISH START
}
}
// GENERATED CLASSFINISH END
// GENERATED SIGNALLER START
import org.osflash.signals.Signal;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.ObjectAddedEvent;
import com.threerings.presents.dobj.ObjectRemovedEvent;
import com.threerings.presents.dobj.OidListListener;
import com.threerings.presents.dobj.SetListener;

import com.threerings.msoy.room.data.RoomObject;

class Signaller
    implements AttributeChangeListener, SetListener, ElementUpdateListener, OidListListener
{
    public function Signaller (obj :RoomObject)
    {
        _obj = obj;
        _obj.addListener(this);
    }

    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        var signal :Signal;
        switch (event.getName()) {
            case "name":
                signal = _obj.nameChanged;
                break;
            case "owner":
                signal = _obj.ownerChanged;
                break;
            case "accessControl":
                signal = _obj.accessControlChanged;
                break;
            case "roomService":
                signal = _obj.roomServiceChanged;
                break;
            case "memories":
                signal = _obj.memoriesChanged;
                break;
            case "controllers":
                signal = _obj.controllersChanged;
                break;
            case "propertySpaces":
                signal = _obj.propertySpacesChanged;
                break;
            case "parties":
                signal = _obj.partiesChanged;
                break;
            case "partyLeaders":
                signal = _obj.partyLeadersChanged;
                break;
            case "playlist":
                signal = _obj.playlistChanged;
                break;
            case "currentSongId":
                signal = _obj.currentSongIdChanged;
                break;
            case "djs":
                signal = _obj.djsChanged;
                break;
            case "currentDj":
                signal = _obj.currentDjChanged;
                break;
            case "track":
                signal = _obj.trackChanged;
                break;
            case "trackRating":
                signal = _obj.trackRatingChanged;
                break;
            case "recentTracks":
                signal = _obj.recentTracksChanged;
                break;
            case "playCount":
                signal = _obj.playCountChanged;
                break;
            default:
                return;
        }
        signal.dispatch(event.getValue(), event.getOldValue());
    }

    public function entryAdded (event :EntryAddedEvent) :void
    {
        var signal :Signal;
        switch (event.getName()) {
            case "memories":
                signal = _obj.memoriesEntryAdded;
                break;
            case "controllers":
                signal = _obj.controllersEntryAdded;
                break;
            case "propertySpaces":
                signal = _obj.propertySpacesEntryAdded;
                break;
            case "parties":
                signal = _obj.partiesEntryAdded;
                break;
            case "partyLeaders":
                signal = _obj.partyLeadersEntryAdded;
                break;
            case "playlist":
                signal = _obj.playlistEntryAdded;
                break;
            case "djs":
                signal = _obj.djsEntryAdded;
                break;
            case "recentTracks":
                signal = _obj.recentTracksEntryAdded;
                break;
            default:
                return;
        }
        signal.dispatch(event.getEntry());
    }

    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        var signal :Signal;
        switch (event.getName()) {
            case "memories":
                signal = _obj.memoriesEntryRemoved;
                break;
            case "controllers":
                signal = _obj.controllersEntryRemoved;
                break;
            case "propertySpaces":
                signal = _obj.propertySpacesEntryRemoved;
                break;
            case "parties":
                signal = _obj.partiesEntryRemoved;
                break;
            case "partyLeaders":
                signal = _obj.partyLeadersEntryRemoved;
                break;
            case "playlist":
                signal = _obj.playlistEntryRemoved;
                break;
            case "djs":
                signal = _obj.djsEntryRemoved;
                break;
            case "recentTracks":
                signal = _obj.recentTracksEntryRemoved;
                break;
            default:
                return;
        }
        signal.dispatch(event.getOldEntry());
    }

    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        var signal :Signal;
        switch (event.getName()) {
            case "memories":
                signal = _obj.memoriesEntryUpdated;
                break;
            case "controllers":
                signal = _obj.controllersEntryUpdated;
                break;
            case "propertySpaces":
                signal = _obj.propertySpacesEntryUpdated;
                break;
            case "parties":
                signal = _obj.partiesEntryUpdated;
                break;
            case "partyLeaders":
                signal = _obj.partyLeadersEntryUpdated;
                break;
            case "playlist":
                signal = _obj.playlistEntryUpdated;
                break;
            case "djs":
                signal = _obj.djsEntryUpdated;
                break;
            case "recentTracks":
                signal = _obj.recentTracksEntryUpdated;
                break;
            default:
                return;
        }
        signal.dispatch(event.getEntry(), event.getOldEntry());
    }

    public function elementUpdated (event :ElementUpdatedEvent) :void
    {
        var signal :Signal;
        switch (event.getName()) {
            default:
                return;
        }
        signal.dispatch(event.getIndex(), event.getValue(), event.getOldValue());
    }

    public function objectAdded (event:ObjectAddedEvent) :void
    {
        var signal :Signal;
        switch (event.getName()) {
            default:
                return;
        }
        signal.dispatch(event.getOid());
    }

    public function objectRemoved (event :ObjectRemovedEvent) :void
    {
        var signal :Signal;
        switch (event.getName()) {
            default:
                return;
        }
        signal.dispatch(event.getOid());
    }

    protected var _obj :RoomObject;
}
// GENERATED SIGNALLER END
