package view
{
	import mx.accessibility.AccImpl;
	import mx.controls.Label;
	import flash.events.Event;
	import flash.accessibility.Accessibility;
	import flash.accessibility.AccessibilityProperties;
	import mx.core.UIComponent;
	
	/* 
		This is an accessibility implementation class for the label, text and image components.
		It was modeled after the other ones in the framework.  Without doing
		this we were getting a "graphic" bug after the label was read.
	*/
	public class RestaurantAccImpl extends AccImpl
	{
		
		private static const EVENT_OBJECT_NAMECHANGE:uint = 0x800C;
		
		/* this is the role constant for non-editable text */
		private const ROLE_SYSTEM_STATICTEXT:uint = 0x08;

		public function RestaurantAccImpl(master:UIComponent)
		{
			super(master);	
			role = ROLE_SYSTEM_STATICTEXT;
		}
		
		/* this is necessary to start up accessibility */
		public static function enableAccessibility():void
		{
		}
		
		/* we would like the screen reader to reread everything when the label changes */
		override protected function get eventsToHandle():Array
		{
			return super.eventsToHandle.concat(["labelChanged"]);
		}
		
		/* This will be called by the screen reader to figure out what to say */
		override protected function getName(childID:uint):String
		{
			var label:String = IAccessibleComponent(master).accessibleText;		
			return label != null && label != "" ? label : "";
		}

		/* This is necessary, otherwise an RTE will be shown */
		override public function get_accState(childID:uint):uint
		{
			var accState:uint = getState(childID);

			return accState;
		}
		
		/* the only event we care about is the label change event */
		override protected function eventHandler(event:Event):void
		{
			switch (event.type)
			{
				/* tell JAWS to update and reread the information */
				case "accessibleTextChanged":
				{
					Accessibility.sendEvent(master, 0, EVENT_OBJECT_NAMECHANGE);
					Accessibility.updateProperties();
					break;
				}
			}
		}
		
	
	}
}