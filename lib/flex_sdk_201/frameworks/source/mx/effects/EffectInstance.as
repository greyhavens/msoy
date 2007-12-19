////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.effects
{

import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.TimerEvent;
import flash.utils.Timer;
import flash.utils.getQualifiedClassName;
import flash.utils.getTimer;
import mx.core.IUIComponent;
import mx.core.mx_internal;
import mx.effects.effectClasses.PropertyChanges;
import mx.events.EffectEvent;
import mx.events.FlexEvent;

use namespace mx_internal;

/**
 *  The EffectInstance class represents an instance of an effect
 *  playing on a target.
 *  Each target has a separate effect instance associated with it.
 *  An effect instance's lifetime is transitory.
 *  An instance is created when the effect is played on a target
 *  and is destroyed when the effect has finished playing. 
 *  If there are multiple effects playing on a target at the same time 
 *  (for example, a Parallel effect), there is a separate effect instance
 *  for each effect.
 * 
 *  <p>Effect developers must create an instance class
 *  for their custom effects.</p>
 *
 *  @see mx.effects.Effect
 */
public class EffectInstance extends EventDispatcher implements IEffectInstance
{
    include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 *
	 *  @param target UIComponent object to animate with this effect.
	 */
	public function EffectInstance(target:Object)
	{
		super();

		this.target = target;
	}

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 *  Timer used to track startDelay and repeatDelay.
	 */
	mx_internal var delayTimer:Timer;
	
	/**
	 *  @private
	 *  Starting time of delayTimer.
	 */
	private var delayStartTime:Number = 0;
	
	/**
	 *  @private
	 *  Elapsed time of delayTimer when paused.
	 *  Used by resume() to figure out amount of time remaining.
	 */
	private var delayElapsedTime:Number = 0;
	
	/**
	 *  @private
	 *  Internal flag remembering whether the user
	 *  explicitly specified a duration or not.
	 */
	mx_internal var durationExplicitlySet:Boolean = false;

	/**
	 *  @private
	 *  If this is a "hide" effect, the EffectManager sets this flag
	 *  as a reminder to hide the object when the effect finishes.
	 */
	mx_internal var hideOnEffectEnd:Boolean = false;
	
	/**
	 *  @private
	 *  Pointer back to the CompositeEffect that created this instance.
	 *  Value is null if we are not the child of a CompositeEffect
	 */
	mx_internal var parentCompositeEffectInstance:EffectInstance;
	
	/** 
	 *  @private
	 *  Number of times that the instance has been played.
	 */
	private var playCount:int = 0;
	
	/**
	 *  @private
	 *  Used internally to prevent the effect from repeating
	 *  once the effect has been ended by calling end().
	 */
	mx_internal var stopRepeat:Boolean = false;

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  actualDuration
	//----------------------------------

	/**
	 *  @private
	 *  Used internally to determine the duration
	 *  including the startDelay, repeatDelay, and repeatCount values.
	 */
	mx_internal function get actualDuration():Number 
	{
		var value:Number = NaN;

		if (repeatCount > 0)
		{
			value = duration * repeatCount +
					(repeatDelay * repeatCount - 1) + startDelay;
		}
		
		return value;
	}
	
	//----------------------------------
	//  className
	//----------------------------------

	/**
	 *  The name of the effect class, such as <code>"FadeInstance"</code>.
	 *
	 *  <p>This is a short or "unqualified" class name
	 *  that does not include the package name.
	 *  If you need the qualified name, use the 
	 *  <code>getQualifiedClassName()</code> method
	 *  in the flash.utils package.</p>
	 */
	public function get className():String
	{
		var name:String = getQualifiedClassName(this);
		
		// If there is a package name, strip it off.
		var index:int = name.indexOf("::");
		if (index != -1)
			name = name.substr(index + 2);
		
		return name;
	}
	
	//----------------------------------
	//  duration
	//----------------------------------

	/**
	 *  @private
	 *  Storage for the duration property.
	 */
	private var _duration:Number = 500;
	
	[Inspectable(category="General", defaultValue="500")]
	
	/** 
	 *  Duration of the effect in milliseconds. 
	 *  @default 500
	 */
	public function get duration():Number
	{
		if (!mx_internal::durationExplicitlySet &&
			mx_internal::parentCompositeEffectInstance)
		{
			return mx_internal::parentCompositeEffectInstance.duration;
		}
		else
		{
			return _duration;
		}
	}
	
	/**
	 *  @private
	 */
	public function set duration(value:Number):void
	{
		mx_internal::durationExplicitlySet = true;
		_duration = value;
	}

	//----------------------------------
	//  effect
	//----------------------------------

	/**
	 *  The Effect object that created this EffectInstance object.
	 */
	private var _effect:Effect;
	public function get effect():Effect
	{
	    return _effect;
	}
	public function set effect(value:Effect):void
	{
	    _effect = value;
	}
	
	//----------------------------------
	//  playheadTime
	//----------------------------------
	
	/**
	 *  Current position in time of the effect.
	 *  This property has a value between 0 and the actual duration 
	 *  (which includes the value of the <code>startDelay</code>, 
	 *  <code>repeatCount</code>, and <code>repeatDelay</code> properties).
	 */
	public function get playheadTime():Number 
	{
		return Math.max(playCount - 1, 0) * duration + 
			   Math.max(playCount - 2, 0) * repeatDelay + 
			   (playReversed ? 0 : startDelay);
	}
	
	//----------------------------------
	//  playReversed
	//----------------------------------

	/**
	 *  @private
	 *  Storage for the playReversed property. 
	 */
	private var _playReversed:Boolean;
	
	/**
	 *  @private
	 *  Used internally to specify whether or not this effect
	 *  should be played in reverse.
	 *  Set this value before you play the effect. 
	 */
	mx_internal function get playReversed():Boolean
	{
		return _playReversed;
	}
	
	/**
	 *  @private
	 */
	mx_internal function set playReversed(value:Boolean):void 
	{
		_playReversed = value;
	}
	
	//----------------------------------
	//  propertyChanges
	//----------------------------------
	
	/**
	 *  Specifies the PropertyChanges object containing the start and end values for 
	 *  the set of properties relevant to the effect's targets. 
	 *  This property is only set if the <code>captureStartValues()</code> method 
	 *  was called on the effect that created this effect instance. 
	 *  
	 *  <p>You often use the <code>propertyChanges</code> property  
	 *  to create an effect that is used as part of a transition. 
	 *  Flex automatically calls the <code>captureStartValues()</code> method 
	 *  when it starts a transition. Within your override of 
	 *  the <code>Effectinstance.play()</code> method, 
	 *  you can examine the information within the 
	 *  <code>propertyChanges()</code> method to initialize the start and end 
	 *  values of the effect. </p>
	 *
	 *  @see mx.effects.effectClasses.PropertyChanges
	 */
	private var _propertyChanges:PropertyChanges;
	public function get propertyChanges():PropertyChanges
	{
	    return _propertyChanges;
	}
	public function set propertyChanges(value:PropertyChanges):void
	{
	    _propertyChanges = value;
	}
	
	//----------------------------------
	//  repeatCount
	//----------------------------------

	/**
	 *  Number of times to repeat the effect.
	 *  Possible values are any integer greater than or equal to 0.
	 *  
	 *  @default 1
	 *  @see mx.effects.Effect#repeatCount
	 */
	private var _repeatCount:int;
	public function get repeatCount():int
	{
	    return _repeatCount;
	}
	public function set repeatCount(value:int):void
	{
	    _repeatCount = value;
	}
	
	//----------------------------------
	//  repeatDelay
	//----------------------------------

	/**
	 *  Amount of time, in milliseconds, to wait before repeating the effect.
	 *  
	 *  @default 0
	 *  @see mx.effects.Effect#repeatDelay
	 */
	private var _repeatDelay:int = 0;
	public function get repeatDelay():int
	{
	    return _repeatDelay;
	}
	public function set repeatDelay(value:int):void
	{
	    _repeatDelay = value;
	}
	
	//----------------------------------
	//  startDelay
	//----------------------------------

	/**
	 *  Amount of time, in milliseconds, to wait before starting the effect.
	 *  Possible values are any int greater than or equal to 0. If the effect
	 *  is repeated by using the <code>repeatCount</code> property, the <code>startDelay</code> property is  
	 *  applied only the first time the effect is played.
	 *  @default 0
	 */
	private var _startDelay:int = 0;
	public function get startDelay():int
	{
	    return _startDelay;
	}
	public function set startDelay(value:int):void
	{
	    _startDelay = value;
	}
	
	//----------------------------------
	//  suspendBackgroundProcessing
	//----------------------------------

	/**
	 *  If <code>true</code>, blocks all background processing
	 *  while the effect is playing.
	 *  Background processing includes measurement, layout, and
	 *  processing responses that have arrived from the server.
	 *  
	 *  @default false
	 *  @see mx.effects.Effect#suspendBackgroundProcessing
	 */
	private var _suspendBackgroundProcessing:Boolean = false;
        public function get suspendBackgroundProcessing():Boolean
	{
	    return _suspendBackgroundProcessing;
	}
	public function set suspendBackgroundProcessing(value:Boolean):void
	{
	    _suspendBackgroundProcessing = value;
	}
	
	//----------------------------------
	//  target
	//----------------------------------

	/**
	 *  The UIComponent object to which this effect is applied.
	 *
	 *  @see mx.effects.Effect#target
	 */
	private var _target:Object;
	public function get target():Object
	{
	    return _target;
	}
	public function set target(value:Object):void
	{
	    _target = value;
	}
	
	//----------------------------------
	//  triggerEvent
	//----------------------------------

	/**
	 *  The event, if any, which triggered the playing of the effect.
	 *  This property is useful when an effect is assigned to 
	 *  multiple triggering events.
	 * 
	 *  <p>If the effect was played programmatically by a call to the 
	 *  <code>play()</code> method,
	 *  rather than being triggered by an event,
	 *  this property is <code>null</code>.</p>
	 */
	private var _triggerEvent:Event;
	public function get triggerEvent():Event
	{
	    return _triggerEvent;
	}
	public function set triggerEvent(value:Event):void
	{
	    _triggerEvent = value;
	}
		
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  This method is called if the effect was triggered by the EffectManager. 
	 *  This base class version saves the event that triggered the effect
	 *  in the <code>triggerEvent</code> property.
	 *  Each subclass should override this method.
	 * 
	 *  @param event The Event object that was dispatched to trigger
	 *  the effect.
	 *  For example, if the trigger was a mouseDownEffect, the event
	 *  would be a MouseEvent with type equal to MouseEvent.MOUSEDOWN. 
	 */
	public function initEffect(event:Event):void
	{
		triggerEvent = event;
		
		switch (event.type)
		{
			case "resizeStart":
			case "resizeEnd":
			{
				if (!mx_internal::durationExplicitlySet)
					duration = 250;
				break;
			}
			
			case FlexEvent.HIDE:
			{
				target.setVisible(true, true);
				hideOnEffectEnd = true;		
				// If somebody else shows us, then cancel the hide when the effect ends
				target.addEventListener(FlexEvent.SHOW, eventHandler); 		
				break;
			}
		}
	}
	
	/**
	 *  Plays the effect instance on the target after the <code>startDelay</code> period
	 *  has elapsed. Called by the Effect class. Use this function instead of
	 *  the <code>play()</code> method when starting an EffectInstance.
	 */
	public function startEffect():void
	{	
		if (target is IUIComponent)
		{
			Object(target).effectStarted(this);
			// Hide the focus ring if the target already has one drawn
			Object(target).drawFocus(false);
		}
		
		if (startDelay > 0 && !playReversed)
		{
			delayTimer = new Timer(startDelay, 1);
			delayStartTime = getTimer();
			delayTimer.addEventListener(TimerEvent.TIMER, delayTimerHandler);
			delayTimer.start();
		}
		else
		{
			play();
		}
	}
			
	/**
	 *  Plays the effect instance on the target. Call the <code>startEffect()</code> 
	 *  method instead to make an effect start playing on an EffectInstance.
	 * 
	 *  <p>In a subclass of EffectInstance, you must override this method. 
	 *  The override must call the <code>super.play()</code> method 
	 *  so that an <code>effectStart</code> event is dispatched from the target.</p>
	 */
	public function play():void
	{
		playCount++;
		
		dispatchEvent(new EffectEvent(EffectEvent.EFFECT_START, false, false, this));
		
		if (target)
			target.dispatchEvent(new EffectEvent(EffectEvent.EFFECT_START, false, false, this));
	}
	
	/**
 	 *  Pauses the effect until you call the <code>resume()</code> method.
  	 */
	public function pause():void
	{	
		if (delayTimer && delayTimer.running && !isNaN(delayStartTime))
		{
			delayTimer.stop(); // Pause the timer
			delayElapsedTime = getTimer() - delayStartTime;
		}
	}
	
	/**
  	 *  Resumes the effect after it has been paused 
  	 *  by a call to the <code>pause()</code> method. 
  	 */
	public function resume():void
	{
		if (delayTimer && !delayTimer.running && !isNaN(delayElapsedTime))
		{
			delayTimer.delay = !playReversed ? delayTimer.delay - delayElapsedTime : delayElapsedTime;
			delayTimer.start();
		}
	}
		
	/**
  	 *  Plays the effect in reverse, starting from the current position of the effect.
  	 */
	public function reverse():void
	{
		if (repeatCount > 0)
			playCount = repeatCount - playCount + 1;
	}
	
	/**
	 *  Interrupts an effect instance that is currently playing,
	 *  and jumps immediately to the end of the effect. This method is
	 *  invoked by a call to the <code>Effect.end()</code> method. 
	 *  As part of its implementation, it calls the 
	 *  <code>finishEffect()</code> method.
	 *
	 *  <p>The effect instance dispatches an <code>effectEnd</code> event
	 *  when you call this method as part of ending the effect. </p>
	 *
	 *  <p>In a subclass of EffectInstance, you can 
	 *  optionally override this method. As part of your override, you should 
	 *  call the <code>super.end()</code> method from the end of your 
	 *  override, after your logic.</p>
	 *
	 *  @see mx.effects.Effect#end()
	 */
	public function end():void
	{
		if (delayTimer)
			delayTimer.reset();
		stopRepeat = true;
		finishEffect();
	}
	
	/**
	 *  Called by the <code>end()</code> method when the effect
	 *  finishes playing.
	 *  This function dispatches an <code>endEffect</code> event
	 *  for the effect target.
	 *
	 *  <p>You do not have to override this method in a subclass.
	 *  You do not need to call this method when using effects,
	 *  but you may need to call it if you create an effect subclass.</p>
	 *
	 *  @see mx.events.EffectEvent
	 */
	public function finishEffect():void
	{
		playCount = 0;
	
		dispatchEvent(new EffectEvent(EffectEvent.EFFECT_END,
									 false, false, this));
		
		if (target)
		{
			target.dispatchEvent(new EffectEvent(EffectEvent.EFFECT_END,
												 false, false, this));
		}
		
		if (target is IUIComponent)
		{
			Object(target).effectFinished(this);
		}
	}

	/**
	 *  Called after each iteration of a repeated effect finishes playing.
	 *
	 *  <p>You do not have to override this method in a subclass.
	 *  You do not need to call this method when using effects.</p>
	 */
	public function finishRepeat():void
	{
		if (!stopRepeat && playCount != 0 &&
			(playCount < repeatCount || repeatCount == 0))
		{
			if (repeatDelay > 0)
			{
				delayTimer = new Timer(repeatDelay, 1);
				delayStartTime = getTimer();
				delayTimer.addEventListener(TimerEvent.TIMER,
											delayTimerHandler);
				delayTimer.start();
			}
			else
			{
				play();
			}
		}
		else
		{
			finishEffect();
		}
	}
	
	
	mx_internal function playWithNoDuration():void
	{
		duration = 0;
		repeatCount = 1;
		repeatDelay = 0;
		startDelay = 0;
		
		startEffect();
	}

	//--------------------------------------------------------------------------
	//
	//  Event handlers
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 *  If someone explicitly sets the visibility of the target object
	 *  to true, clear the flag that is remembering to hide the 
	 *  target when this effect ends.
	 */
	mx_internal function eventHandler(event:Event):void
	{
		if (event.type == FlexEvent.SHOW && hideOnEffectEnd == true)
		{
			hideOnEffectEnd = false;
			event.target.removeEventListener(FlexEvent.SHOW, eventHandler);
		}
	}
	
	/**
	 *  @private
	 */
	private function delayTimerHandler(event:TimerEvent):void
	{
		delayTimer.reset();
		delayStartTime = NaN;
		delayElapsedTime = NaN;
		play();
	}
}

}
