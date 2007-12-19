////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.core
{

/**
 *  The IDeferredInstantiationUIComponent interface defines the interface for a component 
 *  or object that defers instantiation.
 */
public interface IDeferredInstantiationUIComponent extends IUIComponent
{
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  cacheHeuristic
	//----------------------------------

	/**
	 *  @copy mx.core.UIComponent#cacheHeuristic
	 */
    function set cacheHeuristic(value:Boolean):void;

	//----------------------------------
	//  cachePolicy
	//----------------------------------

	/**
	 *  @copy mx.core.UIComponent#cachePolicy
	 */
    function get cachePolicy():String

	//----------------------------------
	//  descriptor
	//----------------------------------

	/**
	 *  @copy mx.core.UIComponent#descriptor
	 */
    function get descriptor():UIComponentDescriptor;
    
	/**
	 *  @private
	 */
    function set descriptor(value:UIComponentDescriptor):void;

	//----------------------------------
	//  id
	//----------------------------------

	/**
	 *  @copy mx.core.UIComponent#id
	 */
    function get id():String;
    
	/**
	 *  @private
	 */
	function set id(value:String):void;

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
     *  Creates an <code>id</code> reference to this IUIComponent object
	 *  on its parent document object.
     *  This function can create multidimensional references
     *  such as b[2][4] for objects inside one or more repeaters.
     *  If the indices are null, it creates a simple non-Array reference.
     *
     *  @param parentDocument The parent of this IUIComponent object. 
	 */
	function createReferenceOnParentDocument(
						parentDocument:IFlexDisplayObject):void;
	
	/**
     *  Deletes the <code>id</code> reference to this IUIComponent object
	 *  on its parent document object.
     *  This function can delete from multidimensional references
     *  such as b[2][4] for objects inside one or more Repeaters.
     *  If the indices are null, it deletes the simple non-Array reference.
     *
     *  @param parentDocument The parent of this IUIComponent object. 
	 */
	function deleteReferenceOnParentDocument(
						parentDocument:IFlexDisplayObject):void;

	/**
	 *  @copy mx.core.UIComponent#executeBindings()
	 */
	function executeBindings(recurse:Boolean = false):void;

	/**
	 *  For each effect event, register the EffectManager
	 *  as one of the event listeners.
	 *
	 *  @param effects An Array of strings of effect names.
	 */
	function registerEffects(effects:Array):void;
}

}
