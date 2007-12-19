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

/**
 *  This interface describes the properties and methods that an object 
 *  must implement so that it can participate in the style subsystem. 
 *  This interface is intended to be used by classes that obtain their
 *  style values from other objects rather than through locally set values
 *  and type selectors.
 *  This interface is implemented by ProgrammaticSkin.
 *
 *  @see mx.styles.IStyleClient
 *  @see mx.styles.CSSStyleDeclaration
 */
public interface ISimpleStyleClient
{
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  styleName
	//----------------------------------

	/**
	 *  The source of this object's style values.
	 *  The value of styleName may be one of three possible types:
	 *
	 *   - A String, such as "headerStyle".
	 *     The string names a class selector defined in a CSS style sheet.
	 *
	 *   - A CSSStyleDeclaration, such as
	 *     StyleManager.getStyleDeclaration(".headerStyle").
	 *
	 *   - A UIComponent. The object implementing this interface inherits all
	 *     the style values from the referenced UIComponent.
	 */
	function get styleName():Object

	/**
	 *  @private
	 */
	function set styleName(value:Object):void
	
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  Called when the value of a style property is changed. 
	 *
     *  @param styleProp The name of the style property that changed. 	 
	 */
	function styleChanged(styleProp:String):void;
}

}
