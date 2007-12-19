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

import flash.events.IEventDispatcher;

[ExcludeClass]

/**
 *  @private
 */
public interface IStyleManager
{
	function getStyleDeclaration(selector:String):CSSStyleDeclaration;
	function setStyleDeclaration(selector:String,
								styleDeclaration:CSSStyleDeclaration,
								update:Boolean):void;
	function clearStyleDeclaration(selector:String, update:Boolean):void;
	function registerInheritingStyle(styleName:String):void;
	function isInheritingStyle(styleName:String):Boolean;
	function isInheritingTextFormatStyle(styleName:String):Boolean;
	function registerSizeInvalidatingStyle(styleName:String):void;
	function isSizeInvalidatingStyle(styleName:String):Boolean;
	function registerParentSizeInvalidatingStyle(styleName:String):void;
	function isParentSizeInvalidatingStyle(styleName:String):Boolean;
	function registerParentDisplayListInvalidatingStyle(styleName:String):void;
	function isParentDisplayListInvalidatingStyle(styleName:String):Boolean;
	function registerColorName(colorName:String, colorValue:uint):void;
	function isColorName(colorName:String):Boolean;
	function getColorName(colorName:Object):uint;
	function getColorNames(colors:Array /* of Number or String */):void;
	function isValidStyleValue(value:*):Boolean;
    function loadStyleDeclarations(url:String,
				   update:Boolean = true,
				   trustContent:Boolean = false):IEventDispatcher;
    function unloadStyleDeclarations(
								url:String, update:Boolean = true):void;

	function initProtoChainRoots():void;
	function styleDeclarationsChanged():void;

	function get stylesRoot():Object;
	function set stylesRoot(value:Object):void;
	function get inheritingStyles():Object;
	function set inheritingStyles(value:Object):void;
	function get typeSelectorCache():Object;
	function set typeSelectorCache(value:Object):void;
}

}

