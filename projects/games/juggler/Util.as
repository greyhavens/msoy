package
{
public class Util {
    
    /** remove an item from an array without making any guarantees about order **/
    public static function removeFromArray(array:Array, target:Object) :void
    {
        for (var i:int=0; i<array.length; i++)
        {
            var candidate:Object = array.shift();
            if (candidate==target) break;
            array.push(candidate);
        }
    }
}
}