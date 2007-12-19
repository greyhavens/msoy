////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.styles
{

import flash.display.DisplayObject;
import mx.core.UIComponent;
import mx.core.UITextField;
import mx.core.mx_internal;
import mx.styles.IStyleClient;

use namespace mx_internal;

[ExcludeClass]

/**
 *  @private
 *  This is an all-static class with methods for building the protochains
 *  that Flex uses to look up CSS style properties.
 */
public class StyleProtoChain
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 *  If the styleName property points to a UIComponent, then we search
	 *  for stylable properties in the following order:
	 *  
	 *  1) Look for inline styles on this object
	 *  2) Look for inline styles on the styleName object
	 *  3) Look for class selectors on the styleName object
	 *  4) Look for type selectors on the styleName object
	 *  5) Look for type selectors on this object
	 *  6) Follow the usual search path for the styleName object
	 *  
	 *  If this object doesn't have any type selectors, then the
	 *  search path can be simplified to two steps:
	 *  
	 *  1) Look for inline styles on this object
	 *  2) Follow the usual search path for the styleName object
	 */
	public static function initProtoChainForUIComponentStyleName(
									obj:IStyleClient):void
	{
		var styleName:IStyleClient = IStyleClient(obj.styleName);
		
		// Push items onto the proto chain in reverse order, beginning with
		// 6) Follow the usual search path for the styleName object
		var nonInheritChain:Object = styleName.nonInheritingStyles;
		if (!nonInheritChain || nonInheritChain == UIComponent.STYLE_UNINITIALIZED)
        {
			nonInheritChain = StyleManager.stylesRoot;

            if (nonInheritChain.effects)
                obj.registerEffects(nonInheritChain.effects);
        }

		var inheritChain:Object = styleName.inheritingStyles;
		if (!inheritChain || inheritChain == UIComponent.STYLE_UNINITIALIZED)
			inheritChain = StyleManager.stylesRoot;

		// If there's no type selector on this object, then we can collapse
		// 6 steps to 2 (see above)
		var typeSelectors:Array = obj.getClassStyleDeclarations();
		var n:int = typeSelectors.length;
		for (var i:int = 0; i < n; i++)
		{
			var typeSelector:CSSStyleDeclaration = typeSelectors[i];

			// If there's no *inheriting* type selector on this object, then we
			// can still collapse 6 steps to 2 for the inheriting properties.

			// 5) Look for type selectors on this object
			inheritChain = typeSelector.addStyleToProtoChain(
				inheritChain, DisplayObject(obj));	

			// 4) Look for type selectors on the styleName object
			// 3) Look for class selectors on the styleName object
			// 2) Look for inline styles on the styleName object
			inheritChain = addProperties(inheritChain, styleName, true);

			// 5) Look for type selectors on this object
			nonInheritChain = typeSelector.addStyleToProtoChain(
				nonInheritChain, DisplayObject(obj));	

			// 4) Look for type selectors on the styleName object
			// 3) Look for class selectors on the styleName object
			// 2) Look for inline styles on the styleName object
			nonInheritChain = addProperties(nonInheritChain, styleName, false);

			if (typeSelector.effects)
				obj.registerEffects(typeSelector.effects);
		}
		
		// 1) Look for inline styles on this object
        
		obj.inheritingStyles =
			obj.styleDeclaration ? 
        	obj.styleDeclaration.addStyleToProtoChain(
				inheritChain, DisplayObject(obj)) :
			inheritChain;
		
		obj.nonInheritingStyles =
			obj.styleDeclaration ? 
			obj.styleDeclaration.addStyleToProtoChain(
				nonInheritChain, DisplayObject(obj)) :
			nonInheritChain;
	}
	
	/**
	 *  See the comment for the initProtoChainForUIComponentStyleName
	 *  function. The comment for that function includes a six-step
	 *  sequence. This sub-function implements the following pieces
	 *  of that sequence:
	 *  
	 *  2) Look for inline styles on the styleName object
	 *  3) Look for class selectors on the styleName object
	 *  4) Look for type selectors on the styleName object
	 *  
	 *   This piece is broken out as a separate function so that it
	 *  can be called recursively when the styleName object has a
	 *  styleName property is itself another UIComponent.
	 */
	private	static function addProperties(chain:Object, obj:IStyleClient,
										  bInheriting:Boolean):Object
	{
		// 4) Add type selectors 
		var typeSelectors:Array = obj.getClassStyleDeclarations();
		var n:int = typeSelectors.length;
		for (var i:int = 0; i < n; i++)
		{
			var typeSelector:CSSStyleDeclaration = typeSelectors[i];
            chain = typeSelector.addStyleToProtoChain(
										chain, DisplayObject(obj));

            if (typeSelector.effects)
                obj.registerEffects(typeSelector.effects);
		}

		// 3) Add class selectors
		var styleName:Object = obj.styleName;
		if (styleName)
		{
			var classSelector:CSSStyleDeclaration;
			
			if (typeof(styleName) == "object")
			{
				if (styleName is CSSStyleDeclaration)
				{
					// Get the style sheet referenced by the styleName property.
					classSelector = CSSStyleDeclaration(styleName);
				}
				else
				{				
					// If the styleName property is another UIComponent, then
					// recursively add type selectors, class selectors, and
					// inline styles for that UIComponent
					chain = addProperties(chain, IStyleClient(styleName),
										  bInheriting);
				}
			}
			else
			{
				// Get the style sheet referenced by the styleName property.
				classSelector =
					StyleManager.getStyleDeclaration("." + styleName);
			}

			if (classSelector)
			{
                chain = classSelector.addStyleToProtoChain(
											chain, DisplayObject(obj));	

				if (classSelector.effects)
					obj.registerEffects(classSelector.effects);
			}
		}		

		// 2) Add inline styles 
        if (obj.styleDeclaration)
            chain = obj.styleDeclaration.addStyleToProtoChain(
											chain, DisplayObject(obj));

		return chain;
	}

	/**
	 *  @private
	 */
	public static function initTextField(obj:UITextField):void
	{
		// TextFields never have any inline styles or type selector, so
		// this is an optimized version of the initObject function (above)
		var styleName:Object = obj.styleName;
		var classSelector:CSSStyleDeclaration;
		
		if (styleName)
		{
			if (typeof(styleName) == "object")
			{
				if (styleName is CSSStyleDeclaration)
				{
					// Get the style sheet referenced by the styleName property.
					classSelector = CSSStyleDeclaration(styleName);
				}
				else
				{				
					// styleName points to a UIComponent, so just set
					// this TextField's proto chains to be the same
					// as that UIComponent's proto chains.
					obj.inheritingStyles =
						IStyleClient(styleName).inheritingStyles;
					obj.nonInheritingStyles =
						IStyleClient(styleName).nonInheritingStyles;
					return;
				}
			}
			else
			{
				// Get the style sheet referenced by the styleName property
				classSelector =
					StyleManager.getStyleDeclaration("." + styleName);
			}
		}
		
		// To build the proto chain, we start at the end and work forward.
		// We'll start by getting the tail of the proto chain, which is:
		//  - for non-inheriting styles, the global style sheet
		//  - for inheriting styles, my parent's style object
		var inheritChain:Object = IStyleClient(obj.parent).inheritingStyles;
		var nonInheritChain:Object = StyleManager.stylesRoot;
		if (!inheritChain)
			inheritChain = StyleManager.stylesRoot;
				
		// Next is the class selector
		if (classSelector)
		{
            inheritChain =
				classSelector.addStyleToProtoChain(inheritChain, obj);

            nonInheritChain =
				classSelector.addStyleToProtoChain(nonInheritChain, obj);	
		}

        obj.inheritingStyles = inheritChain;
        obj.nonInheritingStyles = nonInheritChain;
	}
}

}
