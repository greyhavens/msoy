////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls.videoClasses 
{

import flash.events.Event;
import mx.controls.VideoDisplay;
import mx.core.mx_internal;
import mx.events.MetadataEvent;
import mx.managers.ISystemManager;
import mx.managers.SystemManager;
import mx.resources.ResourceBundle;
import mx.utils.StringUtil;

use namespace mx_internal;

/**
 *  The CuePointManager class lets you use ActionScript code to 
 *  manage the cue points associated with the VideoDisplay control.  
 *
 *  @see mx.controls.VideoDisplay
 */
public class CuePointManager 
{
	include "../../core/Version.as";
	
	//--------------------------------------------------------------------------
	//
	//  Class initialization
	//
	//--------------------------------------------------------------------------

	loadResources();

	//--------------------------------------------------------------------------
	//
	//  Class resources
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	[ResourceBundle("controls")]
    
	/**
	 *  @private
     */	
	private static var packageResources:ResourceBundle;

    /**
	 *  @private
     */	
	private static var resourceWrongTime:String;

    /**
	 *  @private
     */	
	private static var resourceWrongName:String;

    /**
	 *  @private
     */	
	private static var resourceWrongTimeName:String;

    /**
	 *  @private
     */	
	private static var resourceUnsortedCuePoint:String;

    /**
	 *  @private
     */	
	private static var resourceWrongFormat:String;
	
    /**
	 *  @private
     */	
	private static var resourceWrongType:String;

    /**
	 *  @private
     */	
	private static var resourceIncorrectType:String;

    /**
	 *  @private
     */	
	private static var resourceWrongDisabled:String;

    /**
	 *  @private
     */	
	private static var resourceWrongNumParams:String;

    /**
	 *  @private
     */	
	private static var resourceUnexpectedEnd:String;

    /**
	 *  @private
     */	
	private static var resourceCannotDisable:String;

    /**
	 *  @private
     */	
	private static var resourceUndefinedArray:String;

    /**
	 *  @private
     */	
	private static var resourceWrongIndex:String;

    /**
	 *  @private
     */	
	private static var resourceUndefinedParameter:String;

	//--------------------------------------------------------------------------
	//
	//  Class methods
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private    
     *  Loads resources for this class.
     */
	private static function loadResources():void
	{
		resourceWrongTime = packageResources.getString("wrongTime");
		resourceWrongName = packageResources.getString("wrongName");
		resourceWrongTimeName = packageResources.getString("wrongTimeName");
		resourceUnsortedCuePoint = packageResources.getString("unsortedCuePoint");
		resourceWrongFormat = packageResources.getString("wrongFormat");
		resourceWrongType = packageResources.getString("wrongType");
		resourceIncorrectType = packageResources.getString("incorrectType");
		resourceWrongDisabled = packageResources.getString("wrongDisabled"); 
		resourceWrongNumParams = packageResources.getString("wrongNumParams");
		resourceUnexpectedEnd = packageResources.getString("unexpectedEnd");
		resourceCannotDisable = packageResources.getString("cannotDisable");
		resourceUndefinedArray = packageResources.getString("undefinedArray");
		resourceWrongIndex = packageResources.getString("wrongIndex");
		resourceUndefinedParameter = packageResources.getString("undefinedParameter");
	}

    /**
	 *  @private
     */	
	private var _owner:VideoPlayer;

	private var _metadataLoaded:Boolean;
	private var _disabledCuePoints:Array;
	private var _disabledCuePointsByNameOnly:Object;
	private var _cuePointIndex:uint;
	private var _cuePointTolerance:Number;
	private var _linearSearchTolerance:Number;
	private var _id:Number;

	private static var DEFAULT_LINEAR_SEARCH_TOLERANCE:Number = 50;

	private var cuePoints:Array;

	/**
	 *  @private
	 *  Reference to VideoDisplay object associated with this CuePointManager
	 *  instance.
	 */
	mx_internal var videoDisplay:VideoDisplay;

	//
	// public APIs
	//

	/**
	 *  Constructor.
	 */
	public function CuePointManager(owner:VideoPlayer, id:uint = 0) 
    {
		super();

		_owner = owner;
		_id = id;
		reset();
		_cuePointTolerance = _owner.playheadUpdateInterval / 2000;
		_linearSearchTolerance = DEFAULT_LINEAR_SEARCH_TOLERANCE;
	}

	/**
	 *  @private
	 *  Reset cue point lists
	 */
	private function reset():void 
	{
		_metadataLoaded = false;
		cuePoints = null;
		_disabledCuePoints = null;
		_cuePointIndex = 0;
	}

	/**
	 *  @private
	 *  read only, has metadata been loaded
	 */
	private function get metadataLoaded():Boolean
	{
		return _metadataLoaded;
	}

	/**
	 *  @private
	 *  <p>Set by FLVPlayback to update _cuePointTolerance</p>
	 *  Should be exposed in VideoDisplay/ here in Flex 2.0.
	 */
	private function set playheadUpdateInterval(aTime:Number):void
	{ 
		_cuePointTolerance = aTime / 2000;
	}

	/**
	 *  @private 
	 *  <p>corresponds to _vp and _cpMgr array index in FLVPlayback
	 */
	private function get id():Number
	{
		return _id;
	}

	/**
	 *  Adds a cue point.
	 *
	 *  <p>You can add multiple cue points with the same
	 *  name and time.  When you call the <code>removeCuePoint()</code> method 
	 *  with the name and time,  it removes the first matching cue point. 
	 *  To remove all matching cue points, you have to make additional calls to
	 *  the <code>removeCuePoint()</code> method.</p>
	 *
	 *  @param cuePoint The Object describes the cue
	 *  point.  It must contain the properties <code>name:String</code> 
	 *  and <code>time:Number</code> (in seconds).  
	 *  If the Object does not conform to these
	 *  conventions, it throws a <code>VideoError</code> error.
	 *
	 *  @return A copy of the cue point Object added. The copy has the
	 *  following additional properties:
	 *
	 *  <ul>
	 *    <li><code>array</code> - the Array of all cue points. Treat
	 *    this Array as read only because adding, removing or editing objects
	 *    within it can cause cue points to malfunction.</li>
	 * 
	 *    <li><code>index</code> - the index into the Array for the
	 *    returned cue point.</li>
	 *  </ul>
	 * 
	 *  @throws mx.controls.videoClasses.VideoError If the arguments are invalid.
	 */
	public function addCuePoint(cuePoint:Object):Object
	{
		// make sense of param
		var copy:Object = deepCopyObject(cuePoint);

		// sanity check
		var timeUndefined:Boolean = (isNaN(copy.time) || copy.time < 0);
		if (timeUndefined) throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceWrongTime);
		if (!copy.name) throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceWrongName);

		// add cue point to cue point array
		var index:Number;
		copy.type = "actionscript";
		if (cuePoints == null || cuePoints.length < 1)
		{
			index = 0;
			cuePoints = [];
			cuePoints.push(copy);
		}
		else
		{
			index = getCuePointIndex(cuePoints, true, copy.time, null, 0, 0);
			index = (cuePoints[index].time > copy.time) ? 0 : index + 1;
			cuePoints.splice(index, 0, copy);
		}
		
		// adjust _cuePointIndex
		var now:Number = _owner.playheadTime;
		if (now > 0)
		{
			if (_cuePointIndex == index)
			{
				if (now > cuePoints[index].time)
				{
					_cuePointIndex++;
				}
			} 
			else if (_cuePointIndex > index)
			{
				_cuePointIndex++;
			}
		}
		else
		{
			_cuePointIndex = 0;
		}

		// return the cue point
		var returnObject:Object = deepCopyObject(cuePoints[index]);
		returnObject.array = cuePoints;
		returnObject.index = index;
		videoDisplay.dispatchEvent(new Event("cuePointsChanged"));
		return returnObject;
	}

	/**
	 *  Removes a cue point from the currently
	 *  loaded FLV file.  Only the <code>name</code> and <code>time</code> 
	 *  properties are used from the <code>cuePoint</code> argument to 
	 *  determine the cue point to be removed.
	 *
	 *  <p>If multiple cue points match the search criteria, only
	 *  one will be removed.  To remove all cue points, call this function
	 *  repeatedly in a loop with the same arguments until it returns
	 *  <code>null</code>.</p>
	 *
	 *  @param cuePoint The Object must contain at least one of
	 *  <code>name:String</code> and <code>time:Number</code> properties, and
	 *  removes the cue point that matches the specified properties.
	 *
	 *  @return An object representing the cue point removed. If there was no
	 *  matching cue point, then it returns <code>null</code>.
	 */
	public function removeCuePoint(cuePoint:Object):Object 
	{
		// bail if no cue points
		if (cuePoints == null || cuePoints.length < 1)
			return null;

		// remove cue point from cue point array
		var index:Number = getCuePointIndex(cuePoints, false, cuePoint.time, cuePoint.name, 0, 0);
		if (index < 0) 
			return null;

		cuePoint = cuePoints[index];
		cuePoints.splice(index, 1);
		
		// adjust _cuePointIndex
		if (_owner.playheadTime > 0)
		{
			if (_cuePointIndex > index)
			{
				_cuePointIndex--;
			}
		}
		else
		{
			_cuePointIndex = 0;
		}

		videoDisplay.dispatchEvent(new Event("cuePointsChanged"));
		// return the cue point
		return cuePoint;
	}

	/**
	 *  @private	
	 *  removes enabled cue points from _disabledCuePoints
	 */
	private function removeCuePoints(cuePointArray:Array, cuePoint:Object):Number 
	{
		var matchIndex:Number;
		var matchCuePoint:Object;
		var numChanged:Number = 0;
		for (matchIndex = getCuePointIndex(cuePointArray, true, -1, cuePoint.name, 0, 0); matchIndex >= 0;
		     matchIndex = getNextCuePointIndexWithName(matchCuePoint.name, cuePointArray, matchIndex)) 
		{
			// remove match
			matchCuePoint = cuePointArray[matchIndex];
			cuePointArray.splice(matchIndex, 1);
			matchIndex--;
			numChanged++;
		}
		return numChanged;
	}

	/**
	 *  @private	
	 *  inserts cue points into array
	 */
	private function insertCuePoint(insertIndex:Number, cuePointArray:Array, cuePoint:Object):Array 
	{
		if (insertIndex < 0)
		{
			cuePointArray = [];
			cuePointArray.push(cuePoint);
		}
		else
		{
			// find insertion point
			if (cuePointArray[insertIndex].time > cuePoint.time)
			{
				insertIndex = 0;
			}
			else
			{
				insertIndex++;
			}
			// insert into sorted cuePointArray
			cuePointArray.splice(insertIndex, 0, cuePoint);
		}
		return cuePointArray;
	}

	//
	// package internal methods, called by FLVPlayback
	//

	/**
	 *  @private
	 *  <p>Called by FLVPlayback on "playheadUpdate" event
	 *  to throw "cuePoint" events when appropriate.</p>
	 */
	mx_internal function dispatchCuePoints():void
	{
		var now:Number = _owner.playheadTime;
		if (_owner.stateResponsive && cuePoints != null)
		{	
			while (_cuePointIndex < cuePoints.length &&
			        cuePoints[_cuePointIndex].time <= now + _cuePointTolerance)
			{
				var metadataEvent:MetadataEvent =
					new MetadataEvent(MetadataEvent.CUE_POINT);
				metadataEvent.info = deepCopyObject(cuePoints[_cuePointIndex++]);
				_owner.dispatchEvent(metadataEvent);
			}
		}
	}

	/**
	 *  @private
	 *  When our place in the stream is changed, this is called
	 *  to reset our index into actionscript cue point array.
	 *  Another method is used when cue points are added
	 *  are removed.
	 */
	mx_internal function resetCuePointIndex(time:Number):void 
	{
		if (time <= 0 || cuePoints == null)
		{
			_cuePointIndex = 0;
			return;
		}
		var index:Number = getCuePointIndex(cuePoints, true, time, null, 0, 0);
		_cuePointIndex = (cuePoints[index].time < time) ? index + 1 : index;
	}

	/**
	 *  @private
	 *  <p>Process Array passed into FLVPlayback cuePoints property.
	 *  Array actually holds name value pairs.  Each cue point starts
	 *  with 5 pairs: t,time,n,name,t,type,d,disabled,p,numparams.
	 *  time is a Number in milliseconds (e.g. 3000 = 3 seconds), name
	 *  is a String, type is a Number (0 = event, 1 = navigation, 2 =
	 *  actionscript), disabled is a Number (0 for false, 1 for true)
	 *  and numparams is a Number.  After this, there are numparams
	 *  name/value pairs which could be any simple type.</p>
	 *
	 *  <p>Note that all Strings are escaped with html/xml entities for
	 *  ampersand (&amp;), double quote (&quot;), single quote (&#39;)
	 *  and comma (&#44;), so must be unescaped.</p>
	 *
	 *  @see FLVPlayback#cuePoints
	 */
	private function processCuePointsProperty(cuePoints:Array):void
	{
		if (cuePoints == null || cuePoints.length == 0)
		{
			return;
		}

		var state:Number = 0;
		var numParamsLeft:Number;
		var name:String, value:String;
		var cuePoint:Object;
		var disable:Boolean;

		for (var i:uint = 0; i < cuePoints.length; i++)
		{
			switch (state)
			{
			case 6:
				// add cuePoint appropriately
				addOrDisable(disable, cuePoint);
				// reset and process the next
				state = 0;
				// no break
			case 0:
				if (cuePoints[i++] != "t")
				{
					throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceWrongFormat);
				}
				if (isNaN(cuePoints[i]))
				{
					throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceWrongTime);
				}
				cuePoint = {};
				cuePoint.time = cuePoints[i] / 1000;
				state++;
				break;
			case 1:
				if (cuePoints[i++] != "n")
				{
					throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceWrongFormat);
				}
				if (cuePoints[i] == undefined || cuePoints[i] == null)
				{
					throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceWrongName);
				}
				cuePoint.name = unescape(cuePoints[i]);
				state++;
				break;
			case 2:
				if (cuePoints[i++] != "t")
				{
					throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceWrongFormat);
				}
				if (isNaN(cuePoints[i]))
				{
					throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceWrongType);
				}
				switch (cuePoints[i])
				{
				case 0:
					cuePoint.type = "event";
					break;
				case 1:
					cuePoint.type = "navigation";
					break;
				case 2:
					cuePoint.type = "actionscript";
					break;
				default:
					throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceIncorrectType);
				} // switch
				state++;
				break;
			case 3:
				if (cuePoints[i++] != "d")
				{
					throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceWrongFormat);
				}
				if (isNaN(cuePoints[i]))
				{
					throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceWrongDisabled);
				}
				disable = (cuePoints[i] != 0);
				state++;
				break;
			case 4:
				if (cuePoints[i++] != "p")
				{
					throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceWrongFormat);
				}
				if (isNaN(cuePoints[i]))
				{
					throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceWrongNumParams);
				}
				numParamsLeft = cuePoints[i];
				state++;
				if (numParamsLeft == 0)
				{
					state++;
				}
				else
				{
					cuePoint.parameters = {};
				}
				break;
			case 5:
				name = cuePoints[i++];
				value = cuePoints[i];
				if (typeof(name) == "string") name = unescape(name);
				if (typeof(value) == "string") value = unescape(value);
				cuePoint.parameters[name] = value;
				numParamsLeft--;
				if (numParamsLeft == 0) state++;
				break;
			} // switch
		} // for

		// ended badly, throw error
		if (state == 6)
		{
			addOrDisable(disable, cuePoint);
		}
		else
		{
			throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceUnexpectedEnd);
		}
	}


	//
	// private functions
	//

	/**
	 *  @private
	 *  Used by processCuePointsProperty
	 */
	private function addOrDisable(disable:Boolean, cuePoint:Object):void 
	{
		if (disable)
		{
			if (cuePoint.type == "actionscript")
				throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceCannotDisable);
		}
		else if (cuePoint.type == "actionscript")
			addCuePoint(cuePoint);
	}

	private static var cuePointsReplace:Array = [
		"&quot;", "\"",
		"&#39;", "'",
		"&#44;", ",",
		"&amp;", "&"
	];

	/**
	 *  @private
	 *  Used by processCuePointsProperty
	 */
	private function unescape(origStr:String):String
	{
		var newStr:String = origStr;
		for (var i:uint = 0; i < cuePointsReplace.length; i++)
		{
			var broken:Array = newStr.split(cuePointsReplace[i++]);
			if (broken.length > 1)
			{
				newStr = broken.join(cuePointsReplace[i]);
			}
		}
		return newStr;
	}

	/**
	 *  @private
	 *  Search for a cue point in an array sorted by time.  See
	 *  closeIsOK parameter for search rules.
	 *
	 *  @param cuePointArray array to search
	 *  @param closeIsOK If true, the behavior differs depending on the
	 *  parameters passed in:
	 * 
	 *  <ul>
	 *
	 *  <li>If name is null or undefined, then if the specific time is
	 *  not found then the closest time earlier than that is returned.
	 *  If there is no cue point earlier than time, the first cue point
	 *  is returned.</li>
	 *
	 *  <li>If time is null, undefined or less than 0 then the first
	 *  cue point with the given name is returned.</li>
	 *
	 *  <li>If time and name are both defined then the closest cue
	 *  point, then if the specific time and name is not found then the
	 *  closest time earlier than that with that name is returned.  If
	 *  there is no cue point with that name and with an earlier time,
	 *  then the first cue point with that name is returned.  If there
	 *  is no cue point with that name, null is returned.</li>
	 * 
	 *  <li>If time is null, undefined or less than 0 and name is null
	 *  or undefined, a VideoError is thrown.</li>
	 * 
	 *  </ul>
	 *
	 *  <p>If closeIsOK is false the behavior is:</p>
	 *
	 *  <ul>
	 *
	 *  <li>If name is null or undefined and there is a cue point with
	 *  exactly that time, it is returned.  Otherwise null is
	 *  returned.</li>
	 *
	 *  <li>If time is null, undefined or less than 0 then the first
	 *  cue point with the given name is returned.</li>
	 *
	 *  <li>If time and name are both defined and there is a cue point
	 *  with exactly that time and name, it is returned.  Otherwise null
	 *  is returned.</li>
	 *
	 *  <li>If time is null, undefined or less than 0 and name is null
	 *  or undefined, a VideoError is thrown.</li>
	 * 
	 *  </ul>
	 *  @param time search criteria
	 *  @param name search criteria
	 *  @param start index of first item to be searched, used for
	 *  recursive implementation, defaults to 0 if undefined
	 *  @param len length of array to search, used for recursive
	 *  implementation, defaults to cuePointArray.length if undefined
	 *  @returns index for cue point in given array or -1 if no match found
	 *  @throws VideoError if time and/or name parameters are bad
	 *  @see #cuePointCompare()
	 */
	private function getCuePointIndex(cuePointArray:Array, closeIsOK:Boolean,
											  time:Number, name:String,
	                                          start:Number, len:Number):Number 
	{
		// sanity checks		
		if (cuePointArray == null || cuePointArray.length < 1)
			return -1;
		
		var timeUndefined:Boolean = (isNaN(time) || time < 0);
		if (timeUndefined && !name) 
			throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceWrongTimeName);

		if (len == 0) len = cuePointArray.length;

		// name is passed in and time is undefined or closeIsOK is
		// true, search for first name starting at either start
		// parameter index or index at or after passed in time, respectively
		if (name && (closeIsOK || timeUndefined)) 
		{
			var firstIndex:Number;
			var index:Number;
			if (timeUndefined)
				firstIndex = start;
			else 
				firstIndex = getCuePointIndex(cuePointArray, closeIsOK, time, null, 0, 0);
			for (index = firstIndex; index >= start; index--) 
				if (cuePointArray[index].name == name) 
					break;
			if (index >= start) 
				return index;
			for (index = firstIndex + 1; index < len; index++)
				if (cuePointArray[index].name == name) 
					break;
			if (index < len) 
				return index;
			return -1;
		}

		var result:Number;

		// iteratively check if short length
		if (len <= _linearSearchTolerance) 
		{
			var max:Number = start + len;
			for (var i:uint = start; i < max; i++) 
			{
				result = cuePointCompare(time, name, cuePointArray[i]);
				if (result == 0)
					return i;
				if (result < 0) break;
			}
			if (closeIsOK) 
			{
				if (i > 0) 
					return i - 1;
				return 0;
			}
			return -1;
		}

		// split list and recurse
		var halfLen:Number = Math.floor(len / 2);
		var checkIndex:Number = start + halfLen;
		result = cuePointCompare(time, name, cuePointArray[checkIndex]);
		if (result < 0) 
			return getCuePointIndex(cuePointArray, closeIsOK, time, name,
			                         start, halfLen);
		if (result > 0) 
			return getCuePointIndex(cuePointArray, closeIsOK, time, name,
			                         checkIndex + 1, halfLen - 1 + (len % 2));
		return checkIndex;
	}	

	/**
	 *  @private
	 *  <p>Given a name, array and index, returns the next cue point in
	 *  that array after given index with the same name.  Returns null
	 *  if no cue point after that one with that name.  Throws
	 *  VideoError if argument is invalid.</p>
	 *
	 *  @returns index for cue point in given array or -1 if no match found
	 */
	private function getNextCuePointIndexWithName(name:String, array:Array, index:Number):Number 
	{
		// sanity checks
		if (!name)
			throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceWrongName);
		if (!array)
			throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceUndefinedArray);
		if (isNaN(index) || index < -1 || index >= array.length)
			throw new VideoError(VideoError.ILLEGAL_CUE_POINT, 
				resourceWrongIndex);

		// find it
		var i:int;
		for (i = index + 1; i < array.length; i++)
			if (array[i].name == name) 
				break;
		if (i < array.length) 
			return i;
		return -1;
	}

	/**
	 *  @private
	 *  Takes two cue point Objects and returns -1 if first sorts
	 *  before second, 1 if second sorts before first and 0 if they are
	 *  equal.  First compares times with millisecond precision.  If
	 *  they match, compares name if name parameter is not null or undefined.
	 */
	private static function cuePointCompare(time:Number, name:String, cuePoint:Object):Number 
	{
		var compTime1:Number = Math.round(time * 1000);
		var compTime2:Number = Math.round(cuePoint.time * 1000);
		if (compTime1 < compTime2) return -1;
		if (compTime1 > compTime2) return 1;
		if (name != null) 
		{
			if (name == cuePoint.name) return 0;
			if (name < cuePoint.name) return -1;
			return 1;
		}
		return 0;
	}

	/**
	 *  @private
	 *
	 *  <p>Search for a cue point in the given array at the given time
	 *  and/or with given name.</p>
	 *
	 *  @param closeIsOK If true, the behavior differs depending on the
	 *  parameters passed in:
	 * 
	 *  <ul>
	 *
	 *  <li>If name is null or undefined, then if the specific time is
	 *  not found then the closest time earlier than that is returned.
	 *  If there is no cue point earlier than time, the first cue point
	 *  is returned.</li>
	 *
	 *  <li>If time is null, undefined or less than 0 then the first
	 *  cue point with the given name is returned.</li>
	 *
	 *  <li>If time and name are both defined then the closest cue
	 *  point, then if the specific time and name is not found then the
	 *  closest time earlier than that with that name is returned.  If
	 *  there is no cue point with that name and with an earlier time,
	 *  then the first cue point with that name is returned.  If there
	 *  is no cue point with that name, null is returned.</li>
	 * 
	 *  <li>If time is null, undefined or less than 0 and name is null
	 *  or undefined, a VideoError is thrown.</li>
	 * 
	 *  </ul>
	 *
	 *  <p>If closeIsOK is false the behavior is:</p>
	 *
	 *  <ul>
	 *
	 *  <li>If name is null or undefined and there is a cue point with
	 *  exactly that time, it is returned.  Otherwise null is
	 *  returned.</li>
	 *
	 *  <li>If time is null, undefined or less than 0 then the first
	 *  cue point with the given name is returned.</li>
	 *
	 *  <li>If time and name are both defined and there is a cue point
	 *  with exactly that time and name, it is returned.  Otherwise null
	 *  is returned.</li>
	 *
	 *  <li>If time is null, undefined or less than 0 and name is null
	 *  or undefined, a VideoError is thrown.</li>
	 *  
	 *  </ul>
	 *  @param timeOrCuePoint If String, then name for search.  If
	 *  Number, then time for search.  If Object, then cuepoint object
	 *  containing time and/or name parameters for search.
	 *  @returns <code>null</code> if no match was found, otherwise
	 *  copy of cuePoint object with additional properties:
	 *
	 *  <ul>
	 *  
	 *  <li><code>array</code> - the array that was searched. Treat
	 *  this array as read only as adding, removing or editing objects
	 *  within it can cause cue points to malfunction.</li>
	 *
	 *  <li><code>index</code> - the index into the array for the
	 *  returned cuepoint.</li>
	 *
	 *  </ul>
	 *  @see #getCuePointIndex()
	 */
	private function getCuePoint(cuePointArray:Array, closeIsOK:Boolean,
	                      timeNameOrCuePoint:Object = null):Object
	{
		var cuePoint:Object;
		switch (typeof(timeNameOrCuePoint)) 
		{
		case "string":
			cuePoint = {name:timeNameOrCuePoint};
			break;
		case "number":
			cuePoint = {time:timeNameOrCuePoint};
			break;
		case "object":
			cuePoint = timeNameOrCuePoint;
			break;
		} // switch
		var index:Number = getCuePointIndex(cuePointArray, closeIsOK, cuePoint.time, cuePoint.name, 0, 0);
		if (index < 0) return null;
		cuePoint = deepCopyObject(cuePointArray[index]);
		cuePoint.array = cuePointArray;
		cuePoint.index = index;
		return cuePoint;
	}

	/**
	 *  @private
	 *  <p>Given a cue point object returned from getCuePoint (needs
	 *  the index and array properties added to those cue points),
	 *  returns the next cue point in that array after that one with
	 *  the same name.  Returns null if no cue point after that one
	 *  with that name.  Throws VideoError if argument is invalid.</p>
	 *
	 *  @returns <code>null</code> if no match was found, otherwise
	 *  copy of cuePoint object with additional properties:
	 *
	 *  <ul>
	 *  
	 *  <li><code>array</code> - the array that was searched.  Treat
	 *  this array as read only as adding, removing or editing objects
	 *  within it can cause cue points to malfunction.</li>
	 *
	 *  <li><code>index</code> - the index into the array for the
	 *  returned cuepoint.</li>
	 *
	 *  </ul>
	 */
	private function getNextCuePointWithName(cuePoint:Object):Object 
	{
		// sanity checks
		if (!cuePoint)
			throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceUndefinedParameter);
		if (isNaN(cuePoint.time) || cuePoint.time < 0)
			throw new VideoError(VideoError.ILLEGAL_CUE_POINT, resourceWrongTime);

		// get index
		var index:Number = getNextCuePointIndexWithName(cuePoint.name, cuePoint.array, cuePoint.index);
		if (index < 0) 
			return null;

		// return copy
		var returnCuePoint:Object = deepCopyObject(cuePoint.array[index]);
		returnCuePoint.array = cuePoint.array;
		returnCuePoint.index = index;
		return returnCuePoint;
	}

	/**
	 *  Search for a cue point with specified name.
	 *
	 *  @param name The name of the cue point.
	 *  
	 *  @return <code>null</code> if no match was found, or 
	 *  a copy of the matching cue point Object with additional properties:
	 *
	 *  <ul>
	 *    <li><code>array</code> - the Array of cue points searched. Treat
	 *    this array as read only because adding, removing or editing objects
	 *    within it can cause cue points to malfunction.</li>
	 *
	 *    <li><code>index</code> - the index into the Array for the
	 *    returned cue point.</li>
	 *  </ul>
	 */
	public function getCuePointByName(name:String):Object
	{
		return getCuePoint(cuePoints, false, name);
	}
	
	/**
	 *  Returns an Array of all cue points.
	 *
	 *  @return An Array of cue point objects. 
	 *  Each cue point object describes the cue
	 *  point, and contains the properties <code>name:String</code> 
	 *  and <code>time:Number</code> (in seconds).  
	 */
	public function getCuePoints():Array 
	{
		return cuePoints;
	}
	
	/**
	 *  Removes all cue points.
	 */
	public function removeAllCuePoints():void
	{
		cuePoints = null;
		videoDisplay.dispatchEvent(new Event("cuePointsChanged"));
	}
	
	/**
	 * Set the array of cue points.
	 *
	 * <p>You can add multiple cue points with the same
	 * name and time.  When you call the <code>removeCuePoint()</code> method
	 * with this name, only the first one is removed.</p>
	 *
	 *  @param cuePointArray An Array of cue point objects. 
	 *  Each cue point object describes the cue
	 *  point. It must contain the properties <code>name:String</code> 
	 *  and <code>time:Number</code> (in seconds).  
	 */
	public function setCuePoints(cuePointArray:Array):void
	{
		// sanity checks
		if (cuePointArray == null)
			return;
		for (var index:uint=0; index < cuePointArray.length; index++)
			addCuePoint(cuePointArray[index]);
	}
	
	/**
	 *  @private
	 *  Used to make copies of cue point objects.
	 */
	private static function deepCopyObject(obj:Object, recurseLevel:Number = 0):Object 
	{
		if (obj == null || typeof(obj) != "object") return obj;
		if (isNaN(recurseLevel)) recurseLevel = 0;
		var newObj:Object = {};
		for (var i:Object in obj)
		{
			if (recurseLevel == 0 && (i == "array" || i == "index"))
			{
				// skip it
			}
			else if (typeof(obj[i]) == "object")
			{
				newObj[i] = deepCopyObject(obj[i], recurseLevel+1);
			}
			else
			{
				newObj[i] = obj[i];
			}
		}
		return newObj;
	}

} // class mx.controls.videoClasses.CuePointManager

}
