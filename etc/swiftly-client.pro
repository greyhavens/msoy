#
# $Id$
#
# Proguard configuration file for the Swiftly editor client

-injars ../dist/lib/commons-io.jar(!META-INF/*)
-injars ../dist/lib/samskivert.jar(
    com/samskivert/Log.class,**/io/**,**/net/**,**/swing/**,**/text/**,**/util/**,
    **/servlet/user/Password.class,**/servlet/user/User.class,**/servlet/user/UserUtil.class)
-injars ../dist/lib/narya-base.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/narya-distrib.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/nenya-rsrc.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/vilya-whirled.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/vilya-micasa.jar(**/util/**,**/client/**)
-injars ../dist/lib/vilya-parlor.jar(**/parlor/util/**,**/ezgame/data/EZGameConfig.class,
    **/parlor/game/data/GameConfig.class)
-injars ../dist/lib/vilya-stats.jar(!META-INF/*,!**/tools/**,!**/persist/**)
-injars ../dist/lib/threerings.jar(!META-INF/*,**/threerings/util/**)
-injars ../dist/lib/gwt-user.jar(**/user/client/rpc/**)
-injars ../dist/msoy-code.jar(
    !**/*UnitTest.class,rsrc/**,**/msoy/Log.class,**/msoy/data/**,**/msoy/client/**,
    **/msoy/world/data/WorldMemberInfo.class,**/msoy/item/data/all/**,
    **/msoy/swiftly/data/**,**/msoy/swiftly/client/**,**/msoy/swiftly/util/**,
    !**/msoy/game/data/MsoyMatchConfig.class,**/msoy/game/data/*Config.class,
    **/infosys/closeandmaxtabbedpane/**,**/msoy/game/data/GameMemberInfo.class,
    **/msoy/world/data/WorldOccupantInfo.class)
-injars ../dist/lib/sdoc-0.5.0-beta-ooo.jar(!META-INF/*)

-dontskipnonpubliclibraryclasses
-dontobfuscate
-outjars ../dist/swiftly-client.jar

# we need whatever we keep of samskivert to be around in its entirety so
# that if a game uses the same classfile, the whole thing is there

-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    !static !transient <fields>;
    private void writeObject (java.io.ObjectOutputStream);
    private void readObject (java.io.ObjectInputStream);
}

-keep public class * extends com.threerings.presents.dobj.DObject {
    !static !transient <fields>;
}
-keep public class * implements com.threerings.io.Streamable {
    !static !transient <fields>;
    <init> ();
    public void readObject (com.threerings.io.ObjectInputStream);
    public void writeObject (com.threerings.io.ObjectOutputStream);
}

-keep public class * extends java.lang.Enum {
    *;
}

-keep public class com.threerings.msoy.swiftly.client.SwiftlyApplet {
    *;
}
