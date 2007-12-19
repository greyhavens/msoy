/****************************************************************************
* 
* ADOBE CONFIDENTIAL
* ___________________
*
*  Copyright [2002] - [2006] Adobe Macromedia Software LLC and its licensors 
*  All Rights Reserved.
*
* NOTICE:  All information contained herein is, and remains the property
* of Adobe Macromedia Software LLC and its licensors, if any.  
* The intellectual and technical concepts contained herein are proprietary
* to Adobe Macromedia Software LLC and its licensors and may be covered by 
* U.S. and Foreign Patents, patents in process, and are protected by trade  
* secret or copyright law. Dissemination of this information or reproduction 
* of this material is strictly forbidden unless prior written permission is 
* obtained from Adobe Macromedia Software LLC and its licensors.
****************************************************************************/

package com.adobe.viewsource
{

import flash.display.InteractiveObject;
import flash.events.ContextMenuEvent;
import flash.events.Event;
import flash.net.URLRequest;
import flash.net.navigateToURL;
import flash.ui.ContextMenu;
import flash.ui.ContextMenuItem;

public class ViewSource
{
	/**
	 *  Adds a "View Source" context menu item
	 *  to the context menu of the given object.
	 *  Creates a context menu if none exists.
	 *
	 *  @param obj The object to attach the context menu item to.
	 *
	 *  @param url The URL of the source viewer that the "View Source"
	 *  item should open in the browser.
	 *
	 *  @param hideBuiltIns Optional, defaults to true.
	 *  If true, and no existing context menu is attached
	 *  to the given item, then when we create the context menu,
	 *  we hide all the hideable built-in menu items.
	 */
	public static function addMenuItem(obj:InteractiveObject, url:String,
									   hideBuiltIns:Boolean = true):void
	{
		if (obj.contextMenu == null)
		{
			obj.contextMenu = new ContextMenu();
			if (hideBuiltIns)
				obj.contextMenu.hideBuiltInItems();
		}
	
		var item:ContextMenuItem = new ContextMenuItem("View Source");
		
		item.addEventListener(ContextMenuEvent.MENU_ITEM_SELECT, 
			function(event:ContextMenuEvent):void
			{
				if (event.target == item)
					navigateToURL(new URLRequest(url), "_blank");
			}
		);
		
		obj.contextMenu.customItems.push(item);
	}
}
	
}
