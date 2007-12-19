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
import flash.utils.getQualifiedClassName;
import mx.core.IFlexDisplayObject;
import mx.core.mx_internal;
import mx.effects.effectClasses.AddRemoveEffectTargetFilter;
import mx.effects.effectClasses.HideShowEffectTargetFilter;
import mx.effects.effectClasses.PropertyChanges;
import mx.events.EffectEvent;
import mx.managers.LayoutManager;

use namespace mx_internal;

/**
 *  Dispatched when the effect finishes playing,
 *  either when the effect finishes playing or when the effect has 
 *  been interrupted by a call to the <code>end()</code> method.
 *
 *  @eventType mx.events.EffectEvent.EFFECT_END
 */
[Event(name="effectEnd", type="mx.events.EffectEvent")]

/**
 *  Dispatched when the effect starts playing.
 *
 *  @eventType mx.events.EffectEvent.EFFECT_START
 */
[Event(name="effectStart", type="mx.events.EffectEvent")]

/**
 *  The Effect class is an abstract base class that defines the basic 
 *  functionality of all Flex effects.
 *  The Effect class defines the base factory class for all effects.
 *  The EffectInstance class defines the base class for all effect
 *  instance subclasses.
 *
 *  <p>You do not create an instance of the Effect class itself
 *  in an application.
 *  Instead, you create an instance of one of the subclasses,
 *  such as Fade or WipeLeft.</p>
 *  
 *  @mxml
 *
 *  <p>The Effect class defines the following properties,
 *  which all of its subclasses inherit:</p>
 *  
 *  <pre>
 *  &lt;mx:<i>tagname</i>
 *    <b>Properties</b>
 *    customFilter=""
 *    duration="500"
 *    filter=""
 *    repeatCount="1"
 *    repeatDelay="0"
 *    startDelay="0"
 *    suspendBackgroundProcessing="false|true"
 *    target="<i>effect target</i>"
 *    targets="<i>array of effect targets</i>"
 *     
 *    <b>Events</b>
 *    effectEnd="<i>No default</i>"
 *    efectStart="<i>No default</i>"
 *  /&gt;
 *  </pre>
 *
 *  @see mx.effects.EffectInstance
 * 
 *  @includeExample examples/SimpleEffectExample.mxml
 */
public class Effect extends EventDispatcher
{
    include "../core/Version.as";

    //--------------------------------------------------------------------------
    //
    //  Class methods
    //
    //--------------------------------------------------------------------------
    
    /**
     *  @private
     */
    private static function mergeArrays(a1:Array, a2:Array):Array
    {
        if (a2)
        {
            for (var i2:int = 0; i2 < a2.length; i2++)
            {
                var addIt:Boolean = true;
                
                for (var i1:int = 0; i1 < a1.length; i1++)
                {
                    if (a1[i1] == a2[i2])
                    {
                        addIt = false;
                        break;
                    }
                }
                
                if (addIt)
                    a1.push(a2[i2]);
            }
        }
        
        return a1;
    }

    /**
     *  @private
     */
    private static function stripUnchangedValues(propChanges:Array):Array
    {
        // Go through and remove any before/after values that are the same.
        for (var i:int = 0; i < propChanges.length; i++)
        {
            for (var prop:Object in propChanges[i].start)
            {
                if ((propChanges[i].start[prop] ==
                     propChanges[i].end[prop]) ||
                    (typeof(propChanges[i].start[prop]) == "number" &&
                     typeof(propChanges[i].end[prop])== "number" &&
                     isNaN(propChanges[i].start[prop]) &&
                     isNaN(propChanges[i].end[prop])))
                {
                    delete propChanges[i].start[prop];
                    delete propChanges[i].end[prop];
                }
            }
        }
            
        return propChanges;
    }
    
    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     *
     *  <p>Starting an effect is usually a three-step process:</p>
     *
     *  <ul>
     *    <li>Create an instance of the effect object
     *    with the <code>new</code> operator.</li>
     *    <li>Set properties on the effect object,
     *    such as <code>duration</code>.</li>
     *    <li>Call the <code>play()</code> method
     *    or assign the effect to a trigger.</li>
     *  </ul>
     *
     *  @param target The Object to animate with this effect.
     */
    public function Effect(target:Object = null)
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
     */
    private var _instances:Array /* of EffectInstance */ = [];
    
    /**
     *  @private
     */
    private var _callValidateNow:Boolean = false;
        
    private var isPaused:Boolean = false;
    
    /**
     *  @private
     */
    mx_internal var filterObject:EffectTargetFilter;
    
    /**
     *  @private
     */
    mx_internal var applyActualDimensions:Boolean = true; // Used in applyValueToTarget
    
    //--------------------------------------------------------------------------
    //
    //  Properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
    //  className
    //----------------------------------

    /**
     *  The name of the effect class, such as <code>"Fade"</code>.
     *
     *  <p>This is a short, or unqualified, class name
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
    //  customFilter
    //----------------------------------
    
    /**
     *  @private
     *  Storage for the customFilter property.
     */
    private var _customFilter:EffectTargetFilter;
        
    /**
     *  Specifies a custom filter object, of type EffectTargetFilter, used by
     *  the effect to determine the targets on which to play the effect. 
     *  
     *  <p>Target filtering is only performed if you call 
     *  the <code>captureStartValues()</code> method before playing the effect.
     *  Flex automatically calls the <code>captureStartValues()</code> method
     *  when the effect is part of a transition.</p>
     *  
     *  <p>Use the <code>filter</code> property for simple filtering.
     *  If the <code>customFilter</code> property is non-null,
     *  the <code>filter</code> property is ignored.</p>
     *
     *  @default null
     *
     *  @see mx.effects.EffectTargetFilter
     */
    public function get customFilter():EffectTargetFilter
    {
        return _customFilter;
    }

    /**
     *  @private
     */
    public function set customFilter(value:EffectTargetFilter):void
    {
        _customFilter = value;
        mx_internal::filterObject = value;
    }
    
    //----------------------------------
    //  duration
    //----------------------------------

    /**
     *  @private
     *  Storage for the duration property.
     */
    private var _duration:Number = 500;
    
    mx_internal var durationExplicitlySet:Boolean = false;

    [Inspectable(category="General", defaultValue="500")]
    
    /** 
     *  Duration of the effect in milliseconds. 
     *
     *  <p>In a Parallel or Sequence effect, the <code>duration</code> property 
     *  sets the duration of each effect. For example, if a Sequence effect 
     *  has its <code>duration</code> property set to 3000, 
     *  each effect in the Sequence takes 3000 ms to play.</p>
     *
     *  <p>For a repeated effect, the <code>duration</code> property
     *  specifies  the duration of a single instance of the effect. 
     *  Therefore, if an effect has a <code>duration</code> property
     *  set to 2000, and a <code>repeatCount</code> property set to 3, 
     *  the effect takes a total of 6000 ms (6 seconds) to play.</p>
     *
     *  @default 500
     */
    public function get duration():Number
    {
        return _duration;
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
    //  relevantProperties
    //----------------------------------
    
    /**
     *  @private
     *  Storage for the relevantProperties property.
     */
    private var _relevantProperties:Array /* of String */;
        
    /**
     *  An Array of property names to use when performing filtering. 
     *  This property is used internally and should not be set by 
     *  effect users. 
     *
     *  <p>The default value is equal to the Array returned by 
     *  the <code>getAffectedProperties()</code> method.</p>
     */
    public function get relevantProperties():Array /* of String */
    {
        if (_relevantProperties)
            return _relevantProperties;
        else
            return getAffectedProperties();
    }

    /**
     *  @private
     */
    public function set relevantProperties(values:Array /* of String */):void
    {
        _relevantProperties = values;
    }
    
    //----------------------------------
    //  relevantStyles
    //----------------------------------
    
    /**
     *  @private
     */
    private var _relevantStyles:Array /* of String */ = [];
        
    /**
     *  An Array of style names to use when performing filtering. 
     *  This property is used internally and should not be set by 
     *  effect users. 
     *
     *  <p>The default value is equal to the Array returned by 
     *  the <code>getAffectedProperties()</code> method.</p>
     */
    public function get relevantStyles():Array /* of String */
    {
        return _relevantStyles;
    }

    /**
     *  @private
     */
    public function set relevantStyles(values:Array /* of String */):void
    {
        _relevantStyles = values;
    }
    
    //----------------------------------
    //  filter
    //----------------------------------
    
    /**
     *  @private
     *  Storage for the filter property.
     */
    private var _filter:String;
    
    [Inspectable(category="General", enumeration="add,remove,show,hide,move,resize,none", defaultValue="none")]
     
    /**
     *  Specifies an algorithm for filtering targets for an effect. 
     *  A value of null specifies no filtering.
     *  
     *  <p>Target filtering is only performed if you call 
     *  the <code>captureStartValues()</code> method before playing the effect.
     *  Flex automatically calls the <code>captureStartValues()</code> method
     *  when the effect is part of a transition.</p>
     *  
     *  <p>Use this property for simple filtering.
     *  Use the <code>customFilter</code> property for more complex filtering. 
     *  If the <code>customFilter</code> property has a non-null value, 
     *  this property is ignored.</p> 
     *
     *  <p>You can use the following values for the <code>filter</code>
     *  property:</p>
     *
     *  <ul>
     *    <li>A value of <code>"add"</code> plays the effect on any targets 
     *      that are added as a child to a container.</li>
     *    <li>A value of <code>"hide"</code> plays the effect on any targets
     *      whose visible property changed from <code>true</code> to
     *      <code>false</code>.</li>
     *    <li>A value of <code>"move"</code> plays the effect on any targets
     *      that changed their <code>x</code> or <code>y</code>
     *      properties.</li>
     *    <li>A value of <code>"remove"</code> plays the effect on any targets
     *      that are removed as a child of a container.</li>
     *    <li>A value of <code>"resize"</code> plays the effect on any targets 
     *      that changed their <code>width</code> or <code>height</code>
     *      properties.</li>
     *    <li>A value of <code>"show"</code> plays the effect on any targets 
     *      whose visible property changed from <code>false</code> to
     *      <code>true</code>.</li>
     *    <li>A value of <code>""</code> specifies no filtering.</li>
     *  </ul>
     *
     *  @default null
     */
    public function get filter():String
    {
        return _filter;
    }

    /**
     *  @private
     */
    public function set filter(value:String):void
    {
        if (!customFilter)
        {
            _filter = value;
            
            switch (value)
            {
                case "add":
                case "remove":
                {
                    mx_internal::filterObject =
                        new AddRemoveEffectTargetFilter();
                    AddRemoveEffectTargetFilter(mx_internal::filterObject).add =
                        (value == "add");
                    break;
                }
                
                case "hide":
                case "show":
                {
                    mx_internal::filterObject =
                        new HideShowEffectTargetFilter();
                    HideShowEffectTargetFilter(mx_internal::filterObject).show =
                        (value == "show");
                    break;
                }
                
                case "move":
                {
                    mx_internal::filterObject =
                        new EffectTargetFilter();
                    mx_internal::filterObject.filterProperties =
                        [ "x", "y" ];
                    break;
                }
                
                case "resize":
                {
                    mx_internal::filterObject =
                        new EffectTargetFilter();
                    mx_internal::filterObject.filterProperties =
                        [ "width", "height" ];
                    break;
                }
                
                default:
                {
                    mx_internal::filterObject = null;
                    break;          
                }
            }
        }
    }
    
    //----------------------------------
    //  initEvent
    //----------------------------------

    /**
     *  @private    
     *  Contains the Event object passed to this Effect 
     *  by the EffectManager when an effect is triggered.
     */
    mx_internal var initEvent:Event;
    
    //----------------------------------
    //  instanceClass
    //----------------------------------

    /**
     *  An object of type Class that specifies the effect
     *  instance class class for this effect class. 
     *  
     *  <p>All subclasses of the Effect class must set this property 
     *  in their constructor.</p>
     */
	public var instanceClass:Class = IEffectInstance;
    
    //----------------------------------
    //  isPlaying
    //----------------------------------

    /**
     *  A read-only flag which is true if any instances of the effect
     *  are currently playing, and false if none are.
     */
    public function get isPlaying():Boolean
    {
        return _instances && _instances.length > 0;
    }
    
    //----------------------------------
    //  propertyChangesArray
    //----------------------------------

    /**
     *  @private
     *  Holds the init object passed in by the Transition.
     */
    mx_internal var propertyChangesArray:Array; 
    
    //----------------------------------
    //  repeatCount
    //----------------------------------

    [Inspectable(category="General", defaultValue="1")]

    /**
     *  Number of times to repeat the effect.
     *  Possible values are any integer greater than or equal to 0.
     *  A value of 1 means to play the effect once.
     *  A value of 0 means to play the effect indefinitely
     *  until stopped by a call to the <code>end()</code> method.
     *
     *  @default 1
     */
    public var repeatCount:int = 1;
    
    //----------------------------------
    //  repeatDelay
    //----------------------------------

    [Inspectable(category="General", defaultValue="0")]

    /**
     *  Amount of time, in milliseconds, to wait before repeating the effect.
     *  Possible values are any integer greater than or equal to 0.
     *
     *  @default 0
     */
    public var repeatDelay:int = 0;
    
    //----------------------------------
    //  startDelay
    //----------------------------------

    [Inspectable(category="General", defaultValue="0")]

    /**
     *  Amount of time, in milliseconds, to wait before starting the effect.
     *  Possible values are any int greater than or equal to 0.
     *  If the effect is repeated by using the <code>repeatCount</code> property,
     *  the <code>startDelay</code> is only applied to the first time
     *  the effect is played.
     *
     *  @default 0
     */
    public var startDelay:int = 0;
    
    //----------------------------------
    //  suspendBackgroundProcessing
    //----------------------------------

    /**
     *  If <code>true</code>, blocks all background processing
     *  while the effect is playing.
     *  Background processing includes measurement, layout, and
     *  processing responses that have arrived from the server.
     *  The default value is <code>false</code>.
     *
     *  <p>You are encouraged to set this property to
     *  <code>true</code> in most cases, because it improves
     *  the performance of the application.
     *  However, the property should be set to <code>false</code>
     *  if either of the following is true:</p>
     *  <ul>
     *    <li>User input may arrive while the effect is playing,
     *    and the application must respond to the user input
     *    before the effect finishes playing.</li>
     *    <li>A response may arrive from the server while the effect
     *    is playing, and the application must process the response
     *    while the effect is still playing.</li>
     *  </ul>
     *
     *  @default false
     */
    public var suspendBackgroundProcessing:Boolean = false;
    
    //----------------------------------
    //  target
    //----------------------------------

    /** 
     *  The UIComponent object to which this effect is applied.
     *  When an effect is triggered by an effect trigger, 
     *  the <code>target</code> property is automatically set to be 
     *  the object that triggers the effect.
     */
    public function get target():Object
    {
        if (_targets.length > 0)
            return _targets[0]; 
        else
            return null;
    }
    
    /**
     *  @private
     */
    public function set target(value:Object):void
    {
        _targets.splice(0);
        
        if (value)
            _targets[0] = value;
    }

    //----------------------------------
    //  targets
    //----------------------------------

    /**
     *  @private
     *  Storage for the targets property.
     */
    private var _targets:Array = [];
    
    /**
     *  An Array of UIComponent objects that are targets for the effect.
     *  When the effect is playing, it performs the effect on each target
     *  in parallel. 
     *  Setting the <code>target</code> property replaces all objects
     *  in this Array. 
     *  When the <code>targets</code> property is set, the <code>target</code>
     *  property returns the first item in this Array. 
     */
    public function get targets():Array
    {
        return _targets;
    }

    /**
     *  @private
     */
    public function set targets(value:Array):void
    {
        // Strip out null values.
        // Binding will trigger again when the null targets are created.
        var n:int = value.length;
        for (var i:int = n - 1; i > 0; i--)
        {
            if (value[i] == null)
                value.splice(i,1);
        }

        _targets = value;
    }
    
    //--------------------------------------------------------------------------
    //
    //  Methods
    //
    //--------------------------------------------------------------------------
    
    /**
     *  Returns an Array of Strings, where each String is the name
     *  of a property that is changed by this effect.
     *  For example, the Move effect returns an Array that contains
     *  <code>"x"</code> and <code>"y"</code>.
     *
     *  <p>Every subclass of Effect must implement this method.
     *  The method is used by the EffectManager 
     *  to ensure that no two effects are trying to animate
     *  the same property of the same object at the same time.</p>
     *
     *  @return An Array of Strings specifying the names of the 
     *  properties modified by this effect.
     */
    public function getAffectedProperties():Array /* of String */
    {
        // Every subclass should override this method.
        return [];
    }
    
    /**
     *  Begins playing the effect. You typically call the <code>end()</code> method 
     *  before you call the <code>play()</code> method to ensure that any previous 
     *  instance of the effect has ended before you start a new one.
     *
     *  <p>All subclasses must implement this method.</p>
     *
     *  @param targets Array of target objects on which to play this effect. If this
     *  parameter is specified, then the effect's <code>targets</code> property is 
     *  not used.
     *
     *  @param playReversedFromEnd If <code>true</code>,
     *  play the effect backwards.
     *
     *  @return Array of EffectInstance objects, one per target, for the effect.
     */ 
    public function play(targets:Array = null,
                         playReversedFromEnd:Boolean = false):
                         Array /* of EffectInstance */
    {
        // If we have a propertyChangesArray, capture the current values, 
        // strip out any unchanged values, then apply the start values.
        if (targets == null && mx_internal::propertyChangesArray != null)
        {
            if (_callValidateNow)
                LayoutManager.getInstance().validateNow();
            
            mx_internal::propertyChangesArray =
                captureValues(mx_internal::propertyChangesArray, false);
            
            mx_internal::propertyChangesArray =
                stripUnchangedValues(mx_internal::propertyChangesArray);
            
            applyStartValues(mx_internal::propertyChangesArray,
                             this.targets);
        }
        
        var newInstances:Array = createInstances(targets);
                
        var n:int = newInstances.length;
        for (var i:int = 0; i < n; i++) 
        {
			var newInstance:IEffectInstance = IEffectInstance(newInstances[i]);

			Object(newInstance).playReversed = playReversedFromEnd;
            
            newInstance.startEffect();
        }
        
        return newInstances; 
    }
    
    /**
     *  Takes an Array of target objects and invokes the 
     *  <code>createInstance()</code> method on each target. 
     *
     *  @param targets Array of objects to animate with this effect.
     *
     *  @return Array of effect instance objects, one per target, for the effect.
     */
    public function createInstances(targets:Array = null):Array /* of EffectInstance */
    {
        if (!targets)
            targets = this.targets;
            
        var newInstances:Array = [];
        
        // Multiple target support
        var n:int = targets.length;
        for (var i:int = 0; i < n; i++) 
        {
			var newInstance:IEffectInstance = createInstance(targets[i]);
            
            if (newInstance)
                newInstances.push(newInstance);
        }
        
        mx_internal::initEvent = null;
        
        return newInstances; 
    }
    
    /**
     *  Creates a single effect instance and initializes it.
     *  Use this method instead of the <code>play()</code> method
     *  to manipulate the effect instance properties
     *  before the effect instance plays. 
     *  
     *  <p>The effect instance is created with the type 
     *  specified in the <code>instanceClass</code> property.
     *  It is then initialized using the <code>initInstance()</code> method. 
     *  If the instance was created by the EffectManager 
     *  (when the effect is triggered by an effect trigger), 
     *  the effect is further initialized by a call to the 
     *  <code>EffectInstance.initEffect()</code> method.</p>
     * 
     *  <p>Calling the <code>createInstance()</code> method 
     *  does not play the effect.
     *  You must call the <code>startEffect()</code> method
     *  on the returned effect instance. </p>
     *
     *  <p>This function is automatically called by the 
     *  <code>Effect.play()</code> method. </p>
     *
     *  @param target Object to animate with this effect.
     *
     *  @return The effect instance object for the effect.
     */
	public function createInstance(target:Object = null):IEffectInstance
    {       
        if (!target)
            target = this.target;
        
		var newInstance:IEffectInstance = null;
        var props:PropertyChanges = null;
        var create:Boolean = true;
        var setPropsArray:Boolean = false;
                
        if (mx_internal::propertyChangesArray)
        {
            setPropsArray = true;
            create = filterInstance(mx_internal::propertyChangesArray,
                                    target);    
        }
         
        if (create) 
        {
			newInstance = IEffectInstance(new instanceClass(target))
            
            initInstance(newInstance);
            
            if (setPropsArray)
            {
                var n:int = mx_internal::propertyChangesArray.length;
                for (var i:int = 0; i < n; i++)
                {
                    if (mx_internal::propertyChangesArray[i].target == target)
                    {
                        newInstance.propertyChanges =
                            mx_internal::propertyChangesArray[i];
                    }
                }
            }
                
			EventDispatcher(newInstance).addEventListener(EffectEvent.EFFECT_START, effectStartHandler);
			EventDispatcher(newInstance).addEventListener(EffectEvent.EFFECT_END, effectEndHandler);
            
            _instances.push(newInstance);
            
            if (mx_internal::initEvent)
                newInstance.initEffect(mx_internal::initEvent);
        }
        
        return newInstance;
    }

    /**
     *  Copies properties of the effect to the effect instance. 
     *
     *  <p>Flex calls this method from the <code>Effect.createInstance()</code>
     *  method; you do not have to call it yourself. </p>
     *
     *  <p>When you create a custom effect, override this method to 
     *  copy properties from the Effect class to the effect instance class. 
     *  In your override, you must call <code>super.initInstance()</code>. </p>
     *
     *  @param EffectInstance The effect instance to initialize.
     */
	protected function initInstance(instance:IEffectInstance):void
    {
        instance.duration = duration;
		Object(instance).durationExplicitlySet = durationExplicitlySet;
        instance.effect = this;
        instance.repeatCount = repeatCount;
        instance.repeatDelay = repeatDelay;
        instance.startDelay = startDelay;
        instance.suspendBackgroundProcessing = suspendBackgroundProcessing;
    }
    
    /**
     *  Pauses the effect until you call the <code>resume()</code> method.
     */
    public function pause():void
    {   
        if (isPlaying && !isPaused)
        {
            isPaused = true;
            
            var n:int = _instances.length;
            for (var i:int = 0; i < n; i++)
            {
				IEffectInstance(_instances[i]).pause();
            }       
        }
    }
    
    /**
     *  Resumes the effect after it has been paused 
     *  by a call to the <code>pause()</code> method. 
     */
    public function resume():void
    {
        if (isPlaying && isPaused)
        {
            isPaused = false;
            var n:int = _instances.length;
            for (var i:int = 0; i < n; i++)
            {
				IEffectInstance(_instances[i]).resume();
            }
        }
    }
        
    /**
     *  Plays the effect in reverse, if the effect is currently playing,
     *  starting from the current position of the effect.
     */
    public function reverse():void
    {
        if (isPlaying)
        {
            var n:int = _instances.length;
            for (var i:int = 0; i < n; i++)
            {
				IEffectInstance(_instances[i]).reverse();
            }
        }
    }
    
    /**
     *  Interrupts an effect that is currently playing,
     *  and jumps immediately to the end of the effect.
     *  Calling this method invokes the <code>EffectInstance.end()</code>
     *  method.
     *
     *  <p>The effect instance dispatches an <code>effectEnd</code> event
     *  when you call this method as part of ending the effect.</p>
     *
     *  <p>If you pass an effect instance as an argument, 
     *  just that instance is interrupted.
     *  If no argument is passed in, all effect instances currently spawned
     *  from the effect are interrupted.</p>
     *
     *  @param effectInstance EffectInstance to terminate.
     *
     *  @see mx.effects.EffectInstance#end()
     */
	public function end(effectInstance:IEffectInstance = null):void
    {
        if (effectInstance)
        {
            effectInstance.end();
        }
        else
        {
            var n:int = _instances.length;
            for (var i:int = n; i >= 0; i--)
            {
				var instance:IEffectInstance = IEffectInstance(_instances[i]);
                if (instance)
                    instance.end();
            }
        }
    }
    
    /**
     *  Determines the logic for filtering out an effect instance.
     *  The CompositeEffect class overrides this method.
     *
     *  @param propChanges The properties modified by the effect.
     *
     *  @param targ The effect target.
     *
     *  @return Returns <code>true</code> if the effect instance should play.
     */
    protected function filterInstance(propChanges:Array, targ:Object):Boolean 
    {
        if (mx_internal::filterObject)
            return mx_internal::filterObject.filterFunction(propChanges, targ);
        
        return true;
    }
    
    /**
     *  Captures the current values of the relevant properties
     *  on the effect's targets. 
     *  Flex automatically calls the <code>captureStartValues()</code> method
     *  when the effect is part of a transition.
     *  
     *  <p>Use this function when you want the effect to figure out the start 
     *  and end values of the effect.
     *  The proper usage of this function is to use it in the following steps:</p>
     *  
     *  <ol>
     *    <li>Call the <code>captureStartValues()</code> method. 
     *      The effect captures the starting effect values.</li>
     *    <li>Make changes to your effect targets, like adding/removing children, 
     *      altering properties, changing location, or changing dimensions.</li>
     *    <li>Call the <code>play()</code> method.  
     *      The effect captures the end values.
     *      This function populates the
     *      <code>EffectInstance.propertyChanges</code> property
     *      for each effect instance created by this effect. 
     *      Effect developers can use the <code>propertyChanges</code> property 
     *      to retrieve the start and end values for their effect.</li>
     *  </ol>
     */
    public function captureStartValues():void
    {       
        if (targets.length > 0)
        {
            // Reset the PropertyChanges array.
            mx_internal::propertyChangesArray = [];
            _callValidateNow = true;
            
            // Create a new PropertyChanges object for the sum of all targets.
            var n:int = targets.length;
            for (var i:int = 0; i < n; i++)
            {
                mx_internal::propertyChangesArray.push(
                    new PropertyChanges(targets[i]));
            }
            
            mx_internal::propertyChangesArray =
                captureValues(mx_internal::propertyChangesArray,true);
        }
    }
    
    /**
     *  @private
     *  Used internally to grab the values of the relevant properties
     */
    mx_internal function captureValues(propChanges:Array,
                                  setStartValues:Boolean):Array
    {
        // Merge Effect.filterProperties and filterObject.filterProperties
        var effectProps:Array = !mx_internal::filterObject ?
                                relevantProperties :
                                mergeArrays(relevantProperties,
                                mx_internal::filterObject.filterProperties);
        
        var valueMap:Object;
        var target:Object;      
        var n:int;
        var i:int;
        var m:int;  
        var j:int;
        
        // For each target, grab the property's value
        // and put it into the propChanges Array. 
        // Walk the targets.
        if (effectProps && effectProps.length > 0)
        {
            n = propChanges.length;
            for (i = 0; i < n; i++)
            {
                target = propChanges[i].target;
                valueMap = setStartValues ? propChanges[i].start : propChanges[i].end;
                                        
                // Walk the properties in the target
                m = effectProps.length;
                for (j = 0; j < m; j++)
                {
                    valueMap[effectProps[j]] = getValueFromTarget(target,effectProps[j]);
                }
            }
        }
        
        var styles:Array = !mx_internal::filterObject ?
                           relevantStyles :
                           mergeArrays(relevantStyles,
                           mx_internal::filterObject.filterStyles);
        
        if (styles && styles.length > 0)
        {         
            n = propChanges.length;
            for (i = 0; i < n; i++)
            {
                target = propChanges[i].target;
                valueMap = setStartValues ? propChanges[i].start : propChanges[i].end;
                                        
                // Walk the properties in the target.
                m = styles.length;
                for (j = 0; j < m; j++)
                {
                    valueMap[styles[j]] = target.getStyle(styles[j]);
                }
            }
        }
        
        return propChanges;
    }
    
    /**
     *  Called by the <code>captureStartValues()</code> method to get the value
     *  of a property from the target.
     *  This function should only be called internally
     *  by the effects framework.
     *  The default behavior is to simply return <code>target[property]</code>.
     *  Effect developers can override this function
     *  if you need a different behavior. 
     *
     *  @param target The effect target.
     *
     *  @param property The target property.
     *
     *  @return The value of the target property. 
     */
    protected function getValueFromTarget(target:Object, property:String):*
    {
        if (property in target)
            return target[property];
        
        return undefined;
    }
    
    /**
     *  @private
     *  Applies the start values found in the array of PropertyChanges
     *  to the relevant targets.
     */
    mx_internal function applyStartValues(propChanges:Array,
                                     targets:Array):void
    {
        var effectProps:Array = relevantProperties;
                    
        var n:int = propChanges.length;
        for (var i:int = 0; i < n; i++)
        {
            var m:int;
            var j:int;

            var target:Object = propChanges[i].target;
            var apply:Boolean = false;
            
            m = targets.length;
            for (j = 0; j < m; j++)
            {
                if (targets[j] == target)
                {   
                    apply = filterInstance(propChanges, target);
                    break;
                }
            }
            
            if (apply)
            {
                // Walk the properties in the target
                m = effectProps.length;
                for (j = 0; j < m; j++)
                {
                    if (effectProps[j] in propChanges[i].start &&
                        effectProps[j] in target)
                    {
                        applyValueToTarget(target, effectProps[j],
                                propChanges[i].start[effectProps[j]],
                                propChanges[i].start);
                    }
                }
                
                // Walk the styles in the target
                m = relevantStyles.length;
                for (j = 0; j < m; j++)
                {
                    if (relevantStyles[j] in propChanges[i].start)
                        target.setStyle(relevantStyles[j], propChanges[i].start[relevantStyles[j]]);
                }
            }
        }
    }
    
    /**
     *  Used internally by the Effect infrastructure.
     *  If <code>captureStartValues()</code> has been called,
     *  then when Flex calls the <code>play()</code> method, it uses this function
     *  to set the targets back to the starting state.
     *  The default behavior is to take the value captured
     *  using the <code>getValueFromTarget()</code> method
     *  and set it directly on the target's property. For example: <pre>
     *  
     *  target[property] = value;</pre>
     *
     *  <p>Only override this method if you need to apply
     *  the captured values in a different way.
     *  Note that style properties of a target are set
     *  using a different mechanism.
     *  Use the <code>relevantStyles</code> property to specify
     *  which style properties to capture and apply. </p>
     *
     *  @param target The effect target.
     *
     *  @param property The target property.
     *
     *  @param value The value of the property. 
     *
     *  @param props Array of Objects, where each Array element contains a 
     *  <code>start</code> and <code>end</code> Object
     *  for the properties that the effect is monitoring. 
     */
    protected function applyValueToTarget(target:Object, property:String, 
                                          value:*, props:Object):void
    {
        if (property in target)
        {
            // The "property in target" test only tells if the property exists
            // in the target, but does not distinguish between read-only and
            // read-write properties. Put a try/catch around the setter and 
            // ignore any errors.
            try
            {
                
                if (applyActualDimensions && target is IFlexDisplayObject && property == "height")
                {
                    target.setActualSize(target.width,value);
                }
                else if (applyActualDimensions && target is IFlexDisplayObject && property == "width")
                {
                    target.setActualSize(value,target.height);
                }
                else
                {
                    target[property] = value;
                }
            }
            catch (e:Error)
            {
                // Ignore errors
            }
        }
    }

    //--------------------------------------------------------------------------
    //
    //  Event handlers
    //
    //--------------------------------------------------------------------------

    /**
     *  This method is called when the effect instance starts playing. 
     *  If you override this method, ensure that you call the super method. 
     *
     *  @param event An event object of type EffectEvent.
     */
    protected function effectStartHandler(event:EffectEvent):void 
    {
        dispatchEvent(event);
    }
    
    /**
     *  Called when an effect instance has finished playing. 
     *  If you override this method, ensure that you call the super method.
     *
     *  @param event An event object of type EffectEvent.
     */
    protected function effectEndHandler(event:EffectEvent):void 
    {   
		var instance:IEffectInstance = IEffectInstance(event.effectInstance);
        
		EventDispatcher(instance).removeEventListener(EffectEvent.EFFECT_START, effectStartHandler);
		EventDispatcher(instance).removeEventListener(EffectEvent.EFFECT_END, effectEndHandler);
        
        var n:int = _instances.length;
        for (var i:int = 0; i < n; i++)
        {
            if (_instances[i] === instance)
                _instances.splice(i, 1);
        }

        dispatchEvent(event);
    }
}

}
