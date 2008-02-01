package {

public class StateMultiplexor 
{
    public static const SEPARATOR :String = " | ";

    /**
     * Create multiplexed states.
     */
    public static function createStates (
        stateSet1 :Array /* of String */, stateSet2 :Array, ... stateSets) :Array
    {
        stateSets.unshift(stateSet2);
        var newStates :Array = stateSet1.concat(); // make a copy of the first sets

        for each (var stateSet :Array in stateSets) {
            var altStates :Array = [];
            for each (var state :String in stateSet) {
                for each (var oldState :String in newStates) { // LOL!
                    altStates.push(oldState + SEPARATOR + state);
                }
            }
            newStates = altStates;
        }

        return newStates;
    }

    /**
     * Decode a state at a particular set index.
     */
    public static function getState (fullStateString :String, index :int) :String
    {
        return (fullStateString == null) ? null : fullStateString.split(SEPARATOR)[index];
    }
}
}
