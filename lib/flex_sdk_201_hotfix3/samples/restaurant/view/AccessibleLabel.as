package view
{
	import mx.controls.Label;
	import mx.managers.IFocusManagerComponent;
	import flash.events.FocusEvent;
	import flash.events.Event;
	import mx.events.FlexEvent;
	
	/* 
	 * This class extends the mx Label class  in order to make them focusable.
	 * For a user with a screen reader, if the image is critical to the application
	 * you will need to have some way to communicate a summary of the image's information
	 * to a screen reader.
	 *
	 * You can check if a screen reader is being used to make this component
	 * behave like the standard label component so a user that does not need
	 * accessibility features will not see the tooltips or be able to tab
	 * to label components.
	 */
	public class AccessibleLabel extends Label implements IFocusManagerComponent, IAccessibleComponent 
	{
		private var _accessibleText:String = "";
		
		/* everytime the label is changed, we would like JAWS to reread it */
		public function set accessibleText(s:String):void
		{
			_accessibleText = s;
			dispatchEvent(new Event("accessibleTextChanged"));
		}
		public function get accessibleText():String
		{
			return _accessibleText;
		}
		
		public function AccessibleLabel() 
		{
			super();
		}
		
		/* the properties are set on creation complete.  accessibilityProperties will
		 * be guaranteed to be accurate at this point and the accessibility changes
		 * can be made accordingly. */
		override protected function commitProperties():void
		{
			
			/* This checks if accessibility needs to be turned on. */
			if (accessibilityProperties != null) 
			{
				focusEnabled = true;
				tabEnabled = true;
				/* give this component an accessibility implementation to control what is 
				   sent to JAWS */
				accessibilityImplementation = new RestaurantAccImpl(this);
				
				setStyle("focusThickness", 4);
			}
			else 
			{
				focusEnabled = false;
				tabEnabled = false;
				removeEventListener(FocusEvent.FOCUS_IN, focusInHandler);
				removeEventListener(FocusEvent.FOCUS_OUT, focusOutHandler);
			}
		}
		
		override protected function focusInHandler(event:FocusEvent):void 
		{
			/* the information sent to JAWS will be the text for the label if nothing else is provided */
			_accessibleText = (_accessibleText == "" ? text : _accessibleText); 
			drawFocus(true);
			event.stopPropagation();
		}

	}
}