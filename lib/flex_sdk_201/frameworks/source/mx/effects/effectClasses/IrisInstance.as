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

import mx.controls.SWFLoader;

/**
 *  The IrisInstance class implements the instance class for the Iris effect.
 *  Flex creates an instance of this class when it plays an Iris effect;
 *  you do not create one yourself.
 *
 *  @see mx.effects.Iris
 */  
public class IrisInstance extends MaskEffectInstance
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
	public function IrisInstance(target:Object)
	{	
		super(target);
	}

 	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
     */
	override protected function initMaskEffect():void
	{
		super.initMaskEffect();
	
		var targetWidth:Number = targetVisualBounds.width / Math.abs(target.scaleX);
		var targetHeight:Number = targetVisualBounds.height / Math.abs(target.scaleY);
		
		if (target is SWFLoader)
		{
			targetWidth = target.contentWidth;
			targetHeight = target.contentHeight;		
		}
		
		if (showTarget)
		{
			scaleXFrom = 0;
			scaleYFrom = 0;
			scaleXTo = 1;
			scaleYTo = 1;
			
			xFrom = targetWidth / 2 + targetVisualBounds.x;
			yFrom = targetHeight / 2 + targetVisualBounds.y;
			xTo = targetVisualBounds.x;
			yTo = targetVisualBounds.y;
		}
		else
		{
			scaleXFrom = 1;
			scaleYFrom = 1;
			scaleXTo = 0;
			scaleYTo = 0;
			
			xFrom = targetVisualBounds.x;
			yFrom = targetVisualBounds.y;
			xTo = targetWidth / 2 + targetVisualBounds.x;
			yTo = targetHeight / 2 + targetVisualBounds.y;
		}
	}
}

}
