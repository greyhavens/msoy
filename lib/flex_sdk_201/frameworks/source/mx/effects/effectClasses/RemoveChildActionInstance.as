////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.effects.effectClasses
{

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.events.Event;
import mx.core.mx_internal;

/**
 *  The RemoveChildActionInstance class implements the instance class
 *  for the RemoveChildAction effect.
 *  Flex creates an instance of this class when it plays a RemoveChildAction
 *  effect; you do not create one yourself.
 *
 *  @see mx.effects.RemoveChildAction
 */  
public class RemoveChildActionInstance extends ActionEffectInstance
{
    include "../../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 *
	 *  @param target The Object to animate with this effect.
	 */
	public function RemoveChildActionInstance(target:Object)
	{
		super(target);
	}
	
	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	private var _startIndex:Number;

	/**
	 *  @private
	 */
	private var _startParent:DisplayObjectContainer;
	
	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	override public function initEffect(event:Event):void
	{
		super.initEffect(event);
	}
	
	/**
	 *  @private
	 */
	override public function play():void
	{
		var targetDisplayObject:DisplayObject = DisplayObject(target);

		var doRemove:Boolean = true;
		
		// Dispatch an effectStart event from the target.
		super.play();	
		
		if (propertyChanges)
		{
			doRemove = (propertyChanges.start.parent != null &&
						propertyChanges.end.parent == null)
		}
		
		if (!mx_internal::playReversed)
		{
			// Set the style property
			if (doRemove && target && targetDisplayObject.parent != null)
				targetDisplayObject.parent.removeChild(targetDisplayObject);
		}
		else if (_startParent && !isNaN(_startIndex))
		{
			_startParent.addChildAt(targetDisplayObject, _startIndex);
		}
		
		// We're done...
		finishRepeat();
	}
	
	/** 
	 *  @private
	 */
	override protected function saveStartValue():*
	{
		var targetDisplayObject:DisplayObject = DisplayObject(target);

		if (target && targetDisplayObject.parent != null)
		{
			_startIndex =
				targetDisplayObject.parent.getChildIndex(targetDisplayObject);
			_startParent = targetDisplayObject.parent;
		}
	}
}

}
