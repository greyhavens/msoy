////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.utils
{

import flash.utils.Dictionary;
import mx.core.IPropertyChangeNotifier;
import mx.core.IUIComponent;
import mx.core.IUID;
import mx.core.mx_internal;

use namespace mx_internal;

/**
 *  The UIDUtil class is an all-static class
 *  with methods for working with UIDs (unique identifiers) within Flex.
 *  You do not create instances of UIDUtil;
 *  instead you simply call static methods such as the
 *  <code>UIDUtil.createUID()</code> method.
 * 
 *  <p><b>Note</b>: I you have a dynamic object that has no [Bindable] properties 
 *  (which force the object to implement the IUID interface), Flex  adds an 
 *  <code>mx_internal_uid</code> property that contains a UID to the object. 
 *  To avoid having this field 
 *  in your dynamic object, make it [Bindable], implement the IUID interface
 *  in the object class, or set a <coded>uid</coded> property with a value.</p>
 */
public class UIDUtil
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class constants
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
     */
    private static const ALPHA_CHARS:String = "0123456789ABCDEF";

	//--------------------------------------------------------------------------
	//
	//  Class variables
	//
	//--------------------------------------------------------------------------

	/** 
	 *  This Dictionary records all generated uids for all existing items.
	 */
	private static var uidDictionary:Dictionary = new Dictionary(true);

	//--------------------------------------------------------------------------
	//
	//  Class methods
	//
	//--------------------------------------------------------------------------

    /**
	 *  Generates a UID (unique identifier) based on ActionScript's
	 *  pseudo-random number generator and the current time.
	 *
	 *  <p>The UID has the form
	 *  <code>"XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"</code>
	 *  where X is a hexadecimal digit (0-9, A-F).</p>
	 *
	 *  <p>This UID will not be truly globally unique; but it is the best
	 *  we can do without player support for UID generation.</p>
	 *
	 *  @return The newly-generated UID.
     */
    public static function createUID():String
    {
        var uid:String = "";
        
		var i:int;
		var j:int;
		
		for (i = 0; i < 8; i++)
		{
            uid += ALPHA_CHARS.charAt(Math.round(Math.random() *  15));
		}

        for (i = 0; i < 3; i++)
        {
            uid += "-";
            
			for (j = 0; j < 4; j++)
			{
                uid += ALPHA_CHARS.charAt(Math.round(Math.random() * 15));
			}
        }
        
        uid += "-";

		var time:Number = new Date().getTime();
		// Note: time is the number of milliseconds since 1970,
		// which is currently more than one trillion.
		// We use the low 8 hex digits of this number in the UID.
		// Just in case the system clock has been reset to
		// Jan 1-4, 1970 (in which case this number could have only
		// 1-7 hex digits), we pad on the left with 7 zeros
		// before taking the low digits.
        uid += ("0000000" + time.toString(16).toUpperCase()).substr(-8);
        
		for (i = 0; i < 4; i++)
		{
            uid += ALPHA_CHARS.charAt(Math.round(Math.random() * 15));
		}
        
		return uid;
    }
    
    /**
     *  Returns the UID (unique identifier) for the specified object.
     *  If the specified object doesn't have an UID
	 *  then the method assings one to it.
     *  If a map is specified this method will use the map
	 *  to construct the UID.
	 *  As a special case, if the item passed in is null,
	 *  this method returns a null UID.
     *  
     *  @param item Object that we need to find the UID for.
	 *
	 *  @return The UID that was either found or generated.
     */
    public static function getUID(item:Object):String
    {
    	var result:String = null;

        if (item == null)
            return result;

    	if (item is IUID)
    	{
            result = IUID(item).uid;
            if (result == null || result.length == 0)
            {
            	result = createUID();
            	IUID(item).uid = result;
            }
        }
        else if ((item is IPropertyChangeNotifier) &&
				 !(item is IUIComponent))
        {
        	result = IPropertyChangeNotifier(item).uid;
            if (result == null || result.length == 0)
            {
            	result = createUID();
            	IPropertyChangeNotifier(item).uid = result;
            }
        }
		else if (item is String)
		{
			return item as String;
		}
        else
        {
        	try
        	{
        		// We don't create uids for XMLLists, but if
				// there's only a single XML node, we'll extract it.
        		if (item is XMLList && item.length == 1)
        			item = item[0];

				if (item is XML)
				{
					// XML nodes carry their UID on the
					// function-that-is-a-hashtable they can carry around.
					// To decorate an XML node with a UID,
					// we need to first initialize it for notification.
					// There is a potential performance issue here,
					// since notification does have a cost, 
					// but most use cases for needing a UID on an XML node also
					// require listening for change notifications on the node.
					var xitem:XML = XML(item);
					var nodeKind:String = xitem.nodeKind();
					if (nodeKind == "text" || nodeKind == "attribute")
						return xitem.toString();

					var notificationFunction:Function = xitem.notification();
					if (!(notificationFunction is Function))
					{
						// The xml node hasn't already been initialized
						// for notification, so do so now.
						notificationFunction =
							XMLNotifier.initializeXMLForNotification(xitem);						
					}

					// Generate a new uid for the node if necessary.
					if (notificationFunction["uid"] == undefined)
						result = notificationFunction["uid"] = createUID();

					result = notificationFunction["uid"];
				}
				else
				{
					if ("mx_internal_uid" in item)
						return item.mx_internal_uid;

					if ("uid" in item)
						return item.uid;

					result = uidDictionary[item];

					if (!result)
					{
						result = createUID();
						try 
						{
							item.mx_internal_uid = result;
						}
						catch(e:Error)
						{
							uidDictionary[item] = result;
						}
					}
				}
			}
			catch(e:Error)
			{
				result = item.toString();
			}
        }
        	
        return result;
    }
}

}
