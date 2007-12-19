////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls.listClasses
{

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;
import mx.core.FlexShape;
import mx.core.UIComponent;
import mx.core.mx_internal;

use namespace mx_internal;

//--------------------------------------
//  Styles
//--------------------------------------

include "../../styles/metadata/PaddingStyles.as"

/**
 *  Background color of the component.
 */
[Style(name="backgroundColor", type="uint", format="Color", inherit="no")]

//--------------------------------------
//  Other metadata
//--------------------------------------

[ExcludeClass]

/**
 *  @private
 *  The ListBaseContentHolder is the container within a list component
 *  of all of the item renderers and editors.
 *  It is used to mask off areas of the renderers that extend outside
 *  the component and to block certain styles from propagating
 *  down into the renderers so that the renderers have no background
 *  so the highlights and alternating row colors can show through.
 */
public class ListBaseContentHolder extends UIComponent
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
	public function ListBaseContentHolder(parentList:ListBase)
	{
		super();

		this.parentList = parentList;

		setStyle("backgroundColor", "");
		setStyle("borderStyle", "none");
	}

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	private var parentList:ListBase;

	/**
	 *  @private
	 */
	private var maskShape:Shape;

	/**
	 *  @private
	 */
	mx_internal var allowItemSizeChangeNotification:Boolean = true;

	//--------------------------------------------------------------------------
	//
	//  Overridden properties: UIComponent
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  focusPane
	//----------------------------------

	/**
     *  @private
     */
    override public function set focusPane(value:Sprite):void
    {
		if (value)
		{
			// Something inside us is getting focus so apply a clip mask
			// if we don't already have one.
			if (!mask)
			{
				if (!maskShape)
				{
					maskShape = new FlexShape();
					maskShape.name = "mask";

					var g:Graphics = maskShape.graphics;
					g.beginFill(0xFFFFFF);
					g.drawRect(0, 0, width, height);
					g.endFill();

					addChild(maskShape);
				}

				maskShape.visible = false;

				value.mask = maskShape;

			}
		}
		else
		{
			if (focusPane.mask == maskShape)
				focusPane.mask = null;
		}

		super.focusPane = value;
	}

	//--------------------------------------------------------------------------
	//
	//  Overridden methods: UIComponent
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	override public function invalidateSize():void
	{
		if (allowItemSizeChangeNotification)
			parentList.invalidateList();
	}

	/**
	 *  Sets the position and size of the scroll bars and content
	 *  and adjusts the mask.
	 *
	 */
	override protected function updateDisplayList(unscaledWidth:Number,
												  unscaledHeight:Number):void
	{
		super.updateDisplayList(unscaledWidth, unscaledHeight);

		if (maskShape)
		{
			maskShape.width = unscaledWidth;
			maskShape.height = unscaledHeight;
		}
	}

	/**
	 *  @private
	 */
    mx_internal function getParentList():ListBase
    {
        return parentList;
    }

}

}
