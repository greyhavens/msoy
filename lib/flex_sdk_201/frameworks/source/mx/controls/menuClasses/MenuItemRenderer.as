////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls.menuClasses
{

import flash.display.DisplayObject;
import flash.utils.getDefinitionByName;
import mx.controls.Menu;
import mx.controls.listClasses.BaseListData;
import mx.controls.listClasses.IDropInListItemRenderer;
import mx.controls.listClasses.IListItemRenderer;
import mx.controls.listClasses.ListData;
import mx.core.IDataRenderer;
import mx.core.IFlexDisplayObject;
import mx.core.UIComponent;
import mx.core.UITextField;
import mx.core.mx_internal;
import mx.events.FlexEvent;

use namespace mx_internal;

//--------------------------------------
//  Events
//--------------------------------------

/**
 *  Dispatched when the <code>data</code> property changes.
 *
 *  <p>When you use a component as an item renderer,
 *  the <code>data</code> property contains the data to display.
 *  You can listen for this event and update the component
 *  when the <code>data</code> property changes.</p>
 *
 *  @eventType mx.events.FlexEvent.DATA_CHANGE
 */
[Event(name="dataChange", type="mx.events.FlexEvent")]

//--------------------------------------
//  Styles
//--------------------------------------

/**
 *  Text color of the menu item label.
 *  
 *  @default 0x0B333C
 */
[Style(name="color", type="uint", format="Color", inherit="yes")]

/**
 *  Color of the menu item if it is disabled.
 *  
 *  @default 0xAAB3B3
 */
[Style(name="disabledColor", type="uint", format="Color", inherit="yes")]

/**
 *  The MenuItemRenderer class defines the default item renderer
 *  for menu items in any menu control.
 * 
 *  By default, the item renderer draws the text associated
 *  with each menu item, the separator characters, and icons.
 *
 *  <p>You can override the default item renderer
 *  by creating a custom item renderer.</p>
 *
 *  @see mx.controls.Menu
 *  @see mx.controls.MenuBar
 *  @see mx.core.IDataRenderer
 *  @see mx.controls.listClasses.IDropInListItemRenderer
 */
public class MenuItemRenderer extends UIComponent
							  implements IDataRenderer, IListItemRenderer,
							  IMenuItemRenderer, IDropInListItemRenderer
{
	include "../../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function MenuItemRenderer()
	{
		super();
	}

	//--------------------------------------------------------------------------
	//
	//  Constants
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 *  Margin between left side and text
	 *  The icon, if any, is horizontal centered in this space.
	 */
	private var leftMargin:int = 18;

	/**
	 *  @private
	 *  Margin between right side and text
	 *  The branchIcon, if any, is horizontal centered in this space.
	 */
	private var rightMargin:int = 15;

    //--------------------------------------------------------------------------
    //
    //  Properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
	//  branchIcon
    //----------------------------------

	/**
	 *  The internal IFlexDisplayObject that displays the branch icon
	 *  in this renderer.
	 *  
	 *  @default null 
	 */
	protected var branchIcon:IFlexDisplayObject;

    //----------------------------------
	//  data
    //----------------------------------

	/**
	 *  @private
	 *  Storage for the data property.
	 */
	private var _data:Object;

	[Bindable("dataChange")]

	/**
	 *  The implementation of the <code>data</code> property
	 *  as defined by the IDataRenderer interface.
	 *
	 *  @see mx.core.IDataRenderer
	 */
	public function get data():Object
	{
		return _data;
	}

	/**
	 *  @private
	 */
	public function set data(value:Object):void
	{
		_data = value;

		invalidateProperties();
		invalidateSize();

		dispatchEvent(new FlexEvent(FlexEvent.DATA_CHANGE));
	}

    //----------------------------------
	//  icon
    //----------------------------------

	/**
	 *  The internal IFlexDisplayObject that displays the icon in this renderer.
	 *  
	 *  @default null 
	 */
	protected var icon:IFlexDisplayObject;

    //----------------------------------
	//  label
    //----------------------------------

	/**
	 *  The internal UITextField that displays the text in this renderer.
	 * 
	 *  @default null 
	 */
	protected var label:UITextField;

    //----------------------------------
	//  listData
    //----------------------------------

	/**
	 *  @private
	 *  Storage for the listData property.
	 */
	private var _listData:ListData;

	[Bindable("dataChange")]

	/**
	 *  The implementation of the <code>listData</code> property
	 *  as defined by the IDropInListItemRenderer interface.
	 *
	 *  @see mx.controls.listClasses.IDropInListItemRenderer
	 */
	public function get listData():BaseListData
	{
		return _listData;
	}

	/**
	 *  @private
	 */
	public function set listData(value:BaseListData):void
	{
		_listData = ListData(value);

		invalidateProperties();
	}

    //----------------------------------
	//  menu
    //----------------------------------

	/**
	 *  @private
	 *  Storage for the menu property.
	 */
	private var _menu:Menu;

	/**
	 *  Contains a reference to the associated Menu control.
	 * 
	 *  @default null 
	 */
	public function get menu():Menu
	{
		return _menu;
	}

	/**
	 *  @private
	 */
	public function set menu(value:Menu):void
	{
		_menu = value;
	}

	//--------------------------------------------------------------------------
	//
	//  Overridden methods: UIComponent
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	override protected function createChildren():void
	{
        super.createChildren();

		if (!label)
		{
			label = new UITextField();

			label.styleName = this;

			addChild(label);
		}
	}

	/**
	 *  @private
	 */
	override protected function commitProperties():void
	{
		super.commitProperties();

		var iconClass:Class;
		var branchIconClass:Class;

		// Remove any existing icon/branch icon.
		// These will be recreated below if needed.
		if (icon)
		{
			removeChild(DisplayObject(icon));
			icon = null;
		}
		if (branchIcon)
		{
			removeChild(DisplayObject(branchIcon));
			branchIcon = null;
		}

		if (_data)
		{
			var dataDescriptor:IMenuDataDescriptor =
				Menu(_listData.owner).dataDescriptor;
			
			var isEnabled:Boolean = dataDescriptor.isEnabled(_data);
			var type:String = dataDescriptor.getType(_data);

			// Separator
			if (type.toLowerCase() == "separator")
			{
				label.text = "";
				label.visible = false;
				iconClass = getStyle("separatorSkin");
				icon = new iconClass();
				addChild(DisplayObject(icon));
				return;
			}
			else
			{
				label.visible = true;
			}

			// Icon
			if (_listData.icon)
			{
				var listDataIcon:Object = _listData.icon;
				if (listDataIcon is Class)
				{
					iconClass = Class(listDataIcon);
				}
				else if (listDataIcon is String)
				{
					iconClass =
						Class(getDefinitionByName(String(listDataIcon)));
				}

				icon = new iconClass();

				addChild(DisplayObject(icon));
			}	

			// Label
			label.text = _listData.label;

			label.enabled = isEnabled;

			// Check/radio icon
			if (dataDescriptor.isToggled(_data))
			{
				var typeVal:String = dataDescriptor.getType(_data);
				if (typeVal)
				{
					typeVal = typeVal.toLowerCase();
					if (typeVal == "radio")
					{
						iconClass = getStyle(isEnabled ?
											 "radioIcon" :
											 "radioDisabledIcon");
					}
					else if (typeVal == "check")
					{
						iconClass = getStyle(isEnabled ?
											 "checkIcon" :
											 "checkDisabledIcon");
					}

					if (iconClass)
					{
						icon = new iconClass();
						addChild(DisplayObject(icon));
					}
				}
			}

			// Branch icon
			if (dataDescriptor.isBranch(_data))
			{
				branchIconClass = getStyle(isEnabled ?
										   "branchIcon" :
										   "branchDisabledIcon");

				if (branchIconClass)
				{
					branchIcon = new branchIconClass();
					addChild(DisplayObject(branchIcon));
				}
			}
		}
		else
		{
			label.text = " ";
		}
		
		// Invalidate layout here to ensure icons are positioned correctly.
		invalidateDisplayList();
	}

	/**
	 *  @private
	 */
	override protected function measure():void
	{
		super.measure();

		if (icon && leftMargin < icon.measuredWidth)
			leftMargin = icon.measuredWidth;
		
		if (branchIcon && rightMargin < branchIcon.measuredWidth)
			rightMargin = branchIcon.measuredWidth;

		if (isNaN(explicitWidth))
		{
			measuredWidth = label.measuredWidth +
							leftMargin + rightMargin + 7;
		}
		else
		{
			label.width = explicitWidth;
		}

		measuredHeight = label.measuredHeight;
		
		if (icon && icon.measuredHeight > measuredHeight)
			measuredHeight = icon.measuredHeight;
		
		if (branchIcon && branchIcon.measuredHeight > measuredHeight)
			measuredHeight = branchIcon.measuredHeight;
	}

	/**
	 *  @private
	 */
	override protected function updateDisplayList(unscaledWidth:Number,
												  unscaledHeight:Number):void
	{
		super.updateDisplayList(unscaledWidth, unscaledHeight);

		if(_listData)
		{
			if (Menu(_listData.owner).dataDescriptor.
				getType(_data).toLowerCase() == "separator")
			{
				if (icon)
				{
					icon.x = 2;
					icon.y = (unscaledHeight - icon.measuredHeight) / 2;
					icon.setActualSize(unscaledWidth - 4, icon.measuredHeight);
				}
				return;
			}

			if (icon)
			{
				icon.x = (leftMargin - icon.measuredWidth) / 2;
				icon.setActualSize(icon.measuredWidth, icon.measuredHeight);
			}

			label.x = leftMargin;
			
			label.setActualSize(unscaledWidth - leftMargin - rightMargin,
								label.getExplicitOrMeasuredHeight());

			if (_listData && !Menu(_listData.owner).showDataTips)
			{
				label.text = _listData.label;
				if (label.truncateToFit())
					toolTip = _listData.label;
				else
					toolTip = null;
			}

			if (branchIcon)
			{
				branchIcon.x = unscaledWidth - rightMargin +
							   (rightMargin - branchIcon.measuredWidth) / 2;
				
				branchIcon.setActualSize(branchIcon.measuredWidth,
										 branchIcon.measuredHeight);
			}

			var verticalAlign:String = getStyle("verticalAlign");
			if (verticalAlign == "top")
			{
				label.y = 0;
				if (icon)
					icon.y = 0;
				if (branchIcon)
					branchIcon.y = 0;
			}
			else if (verticalAlign == "bottom")
			{
				label.y = unscaledHeight - label.height + 2; // 2 for gutter
				if (icon)
					icon.y = unscaledHeight - icon.height;
				if (branchIcon)
					branchIcon.y = unscaledHeight - branchIcon.height;
			}
			else
			{
				label.y = (unscaledHeight - label.height) / 2;
				if (icon)
					icon.y = (unscaledHeight - icon.height) / 2;
				if (branchIcon)
					branchIcon.y = (unscaledHeight - branchIcon.height) / 2;
			}

			var labelColor:Number;

			if (data && parent)
			{
				if (!enabled)
				{
					labelColor = getStyle("disabledColor");
				}
				else if (Menu(listData.owner).isItemHighlighted(listData.uid))
				{
					labelColor = getStyle("textRollOverColor");
				}
				else if (Menu(listData.owner).isItemSelected(listData.uid))
				{
					labelColor = getStyle("textSelectedColor");
				}
				else
				{
					labelColor = getStyle("color");
				}

				label.setColor(labelColor);
			}
		}
	}

	/**
	 *  @private
	 */
	override public function styleChanged(styleProp:String):void
	{
		super.styleChanged(styleProp);

		if (!styleProp ||
			styleProp == "styleName" ||
			(styleProp.toLowerCase().indexOf("icon") != -1))
		{
			// If any icons change, invalidate everything.
			// We could be smarter about this if it causes
			// performance problems.
			invalidateSize();
			invalidateDisplayList();
		}
	}

    /**
     *  @private
     */
    mx_internal function getLabel():UITextField
    {
        return label;
    }
}

}
