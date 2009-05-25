//
// $Id$

package com.threerings.msoy.chat.client {

import mx.core.mx_internal;

import com.whirled.ui.NameLabelCreator;
import com.whirled.ui.PlayerList;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.ui.MsoyNameLabelCreator;

/**
 * Displays a list of room occupants.
 */
public class RoomOccupantList extends PlayerList
    implements SetListener
{
    public function RoomOccupantList (ctx :MsoyContext)
    {
        super(new MsoyNameLabelCreator(ctx, true));

        _list.mouseEnabled = false;
        _list.mx_internal::getListContentHolder().mouseEnabled = false;
        mouseEnabled = false;
    }

    public function havePlace () :Boolean
    {
        return _plobj != null;
    }

    public function setPlaceObject (plobj: PlaceObject) :void
    {
        if (_plobj != null) {
            // clear out our old place
            _plobj.removeListener(this);
            _plobj = null;
            // clear out any occupants in our list
            clear();
        }

        if (plobj != null) {
            // listen for changes on our place object
            _plobj = plobj;
            _plobj.addListener(this);

            // set up our current occupants
            for each (var occInfo :OccupantInfo in plobj.occupantInfo.toArray()) {
                addOccupant(occInfo);
            }
        }
    }

    // from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == PlaceObject.OCCUPANT_INFO) {
            addOccupant((event.getEntry() as OccupantInfo));
        }
    }

    // from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        if (event.getName() == PlaceObject.OCCUPANT_INFO) {
            itemUpdated(new RoomOccupantRecord(event.getEntry() as OccupantInfo));
        }
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        if (event.getName() == PlaceObject.OCCUPANT_INFO) {
            removeItem(new RoomOccupantRecord(event.getOldEntry() as OccupantInfo));
        }
    }

    protected function addOccupant (occInfo :OccupantInfo) :void
    {
        // only members get to be in the occupant list (sorry pets...)
        if (occInfo is MemberInfo) {
            addItem(new RoomOccupantRecord(occInfo));
        }
    }

    override protected function getRenderingClass () :Class
    {
        return RoomOccupantRenderer;
    }

    protected var _plobj :PlaceObject;
}
}

import flash.display.DisplayObject;

import mx.containers.HBox;

import mx.core.ScrollPolicy;

import com.threerings.util.Comparable;
import com.threerings.util.Hashable;
import com.threerings.util.Name;

import com.threerings.crowd.data.OccupantInfo;

import com.whirled.ui.NameLabel;
import com.whirled.ui.NameLabelCreator;

import com.threerings.msoy.data.MsoyUserOccupantInfo;
import com.threerings.msoy.data.all.VizMemberName;

class RoomOccupantRecord
    implements Hashable
{
    /** The username. */
    public var name :Name;

    /** The subscriber status. */
    public var subscriber :Boolean;

    public function RoomOccupantRecord (occInfo :OccupantInfo)
    {
        this.name = occInfo.username;
        this.subscriber = (occInfo is MsoyUserOccupantInfo) &&
            MsoyUserOccupantInfo(occInfo).isSubscriber();
    }

    // from Hashable
    public function hashCode () :int
    {
        return name.hashCode();
    }

    // from Equalable (via Hashable)
    public function equals (other :Object) :Boolean
    {
        return (other is RoomOccupantRecord) && name.equals(RoomOccupantRecord(other).name);
    }

    // from Comparable
    public function compareTo (other :Object) :int
    {
        var that :RoomOccupantRecord = RoomOccupantRecord(other);
        if (this.subscriber != that.subscriber) {
            // subscribers sort to the top
            return this.subscriber ? 1 : -1;

        } else {
            // otherwise: based on name
            return this.name.compareTo(that.name);
        }
    }
}

class RoomOccupantRenderer extends HBox
{
    public function RoomOccupantRenderer ()
    {
        super();

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        setStyle("backgroundAlpha", 0);
        mouseEnabled = false;
    }

    override public function set data (value :Object) :void
    {
        super.data = value;

        if (processedDescriptors) {
            configureUI();
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        configureUI();
    }

    protected function configureUI () :void
    {
        if (this.data != null && (this.data is Array) && (this.data as Array).length == 2) {
            var dataArray :Array = this.data as Array;
            var creator :NameLabelCreator = dataArray[0] as NameLabelCreator;
            var record :RoomOccupantRecord = dataArray[1] as RoomOccupantRecord;
            var name :VizMemberName = record.name as VizMemberName;
            if (_currentName == null || !_currentName.equals(name) ||
                _currentName.toString() != name.toString() ||
                !_currentName.getPhoto().equals(name.getPhoto())) {
                if (_currentLabel != null && contains(DisplayObject(_currentLabel))) {
                    removeChild(DisplayObject(_currentLabel));
                }
                _currentLabel = creator.createLabel(name, record.subscriber);
                addChild(DisplayObject(_currentLabel));
                _currentLabel.percentWidth = 100;
                _currentName = name;
            }
        } else {
            if (_currentLabel != null && contains(DisplayObject(_currentLabel))) {
                removeChild(DisplayObject(_currentLabel));
            }
            _currentLabel = null;
            _currentName = null;
        }
    }

    protected var _currentLabel :NameLabel;
    protected var _currentName :VizMemberName;
}
