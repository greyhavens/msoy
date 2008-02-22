////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.effects
{

import flash.events.Event;
import flash.events.IEventDispatcher;

/**
 *  The IAbstractEffect interface is used to denote
 *  that a property or parameter must be of type Effect,
 *  but does not actually implement any of the APIs of the 
 *  IEffect interface.
 *  The UIComponent class recognizes when property that 
 *  implements the AbstractEffect interface changes, and passes it to 
 *  the EffectManager class for processing.
 *
 *  @see mx.effects.IEffect
 */
public interface IAbstractEffect extends IEventDispatcher
{
}

}