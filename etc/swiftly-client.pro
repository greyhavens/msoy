#
# $Id$
#
# Proguard configuration file for the Swiftly editor client

-injars ../dist/lib/commons-io.jar(!META-INF/*)
-injars ../dist/lib/narya-base.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/narya-distrib.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/vilya-whirled.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/vilya-micasa.jar(**/micasa/client/ChatPanel*,**/micasa/client/OccupantList*)
-injars ../dist/lib/vilya-parlor.jar(!META-INF/*,**/data/**,**/client/*Service*.class)
-injars ../dist/lib/samskivert.jar(!META-INF/*,!**/velocity/**,!**/xml/**)
-injars ../dist/lib/threerings.jar(!META-INF/*,**/threerings/util/**)
-injars ../dist/lib/toybox.jar(**/data/ToyBoxMarshaller*.class,**/client/ToyBoxService*.class)
-injars ../dist/lib/gwt-user.jar(**/user/client/rpc/**)
-injars ../dist/msoy-code.jar(
  !META-INF/*,
  !**/tools/**,
  !**/server/**,
  !**/world/**,
  !**/item/web/**,
  !**/item/server/**,
  !**/game/server/**,
  !**/game/client/Game*,
  !**/game/data/MsoyGame*,
  !**/msoy/web/**,
  !**/xml/**,
  !com/threerings/io/**)
-injars ../dist/msoy-code.jar(
  **/msoy/web/data/**,
  **/msoy/item/web/**,
  **/msoy/world/data/PetMarshaller*,
  **/msoy/world/client/PetService*
)

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
