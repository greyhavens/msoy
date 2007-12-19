package view
{
	import mx.controls.Text;
	import mx.managers.IFocusManagerComponent;
	import flash.events.FocusEvent;
	import flash.events.Event;
	import mx.events.FlexEvent;
	
	/* 
	 * This class extends the mx Image class  in order to make them focusable.
	 * For a user with a screen reader, if the image is critical to the application
	 * you will need to have some way to communicate a summary of the image's information
	 * to a screen reader.
	 *
	 * You can check if a screen reader is being used to make this component
	 * behave like the standard text component so a user that does not need
	 * accessibility features will not see the tooltips or be able to tab
	 * to text components.
	 */
	public class AccessibleText extends Text implements IFocusManagerComponent, IAccessibleComponent
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
		
		public function AccessibleText() 
		{
			super();
			this.addEventListener(FlexEvent.CREATION_COMPLETE, init);
		}
		
		/* the properties are set on creation complete.  accessibilityProperties will
		 * be guaranteed to be accurate at this point and the accessibility changes
		 * can be made accordingly. */
		private function init(event:Event):void
		{			
			/* This checks if accessibility needs to be turned on */
			if (this.accessibilityProperties != null) 
			{
				this.focusEnabled = true;
				this.tabEnabled = true;
				/* give this component an accessibility implementation to control what is 
				   sent to JAWS */
				this.accessibilityImplementation = new RestaurantAccImpl(this);
				
				this.setStyle("focusThickness", 4);
			}
			else 
			{
				this.focusEnabled = false;
				this.tabEnabled = false;
				this.removeEventListener(FocusEvent.FOCUS_IN, focusInHandler);
				this.removeEventListener(FocusEvent.FOCUS_OUT, focusOutHandler);
			}
		}
		
		override protected function focusInHandler(event:FocusEvent):void 
		{
			/* the information sent to JAWS will be the text for the label if nothing else is provided */
			_accessibleText = (_accessibleText == "" ? text : _accessibleText); 
			this.drawFocus(true);
			event.stopPropagation();
		}
	}
}