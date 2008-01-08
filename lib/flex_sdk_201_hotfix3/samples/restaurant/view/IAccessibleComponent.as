package view
{
	/* a simple interface for all components that don't have an accessiblity implementation
	   and you would like to be able to control what is read by JAWS */
	public interface IAccessibleComponent
	{
		function get accessibleText():String;
		function set accessibleText(s:String):void;
	}
}