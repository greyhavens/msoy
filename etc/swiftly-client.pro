#
# $Id$
#
# Proguard configuration file for the Swiftly editor client

-injars ../dist/lib/commons-io.jar(!META-INF/*)
-injars ../dist/lib/samskivert-0.0-SNAPSHOT.jar(
    com/samskivert/Log.class,**/io/**,**/net/**,**/swing/**,**/text/**,**/util/**,
    **/servlet/user/Password.class,**/servlet/user/User.class,**/servlet/user/UserUtil.class)
-injars ../dist/lib/narya-base-0.0-SNAPSHOT.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/narya-distrib-0.0-SNAPSHOT.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/nenya-rsrc-0.0-SNAPSHOT.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/vilya-whirled-0.0-SNAPSHOT.jar(
    **/ClusteredBodyObject.class,**/ScenedBodyObject.class)
-injars ../dist/lib/vilya-micasa-0.0-SNAPSHOT.jar(**/util/**,**/client/**)
-injars ../dist/lib/vilya-parlor-0.0-SNAPSHOT.jar(**/parlor/util/**)
-injars ../dist/lib/vilya-stats-0.0-SNAPSHOT.jar(!META-INF/*,!**/tools/**,!**/persist/**)
-injars ../dist/lib/threerings-0.0-SNAPSHOT.jar(
    !META-INF/*,!**/OOOFileAppender*.class,**/threerings/util/**)
-injars ../dist/lib/whirled-code-0.0-SNAPSHOT.jar(**/WhirledOccupantInfo.class)
-injars ../dist/lib/gwt-user.jar(**/user/client/rpc/IsSerializable.class,
    **/user/client/rpc/SerializableException.class)
-injars ../dist/msoy-code.jar(
    !**/*UnitTest.class,rsrc/i18n/**,**/msoy/Log.class,**/msoy/data/**,**/msoy/client/**,
    **/msoy/room/data/WorldMemberInfo.class,**/msoy/item/data/all/**,
    **/msoy/swiftly/data/**,**/msoy/swiftly/client/**,**/msoy/swiftly/util/**,
    **/msoy/game/data/GameMemberInfo.class,**/msoy/game/data/GameSummary.class,
    **/msoy/notify/data/**,**/msoy/room/data/WorldOccupantInfo.class)
-injars ../dist/msoy-media.jar(**/icons/swiftly/**)
-injars ../dist/lib/sdoc-0.5.0-beta-ooo.jar(!META-INF/*)
-injars ../dist/lib/substance-lite.jar(!META-INF/*)
-injars ../dist/lib/google-collect.jar(!META-INF/*)

-dontskipnonpubliclibraryclasses
-dontobfuscate
-outjars ../dist/swiftly-client.jar

-keep class * extends javax.swing.plaf.ComponentUI {
    public static javax.swing.plaf.ComponentUI createUI(javax.swing.JComponent);
}

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
    public void readField_* (com.threerings.io.ObjectInputStream);
    public void writeField_* (com.threerings.io.ObjectOutputStream);
}

-keep public class * extends java.lang.Enum {
    *;
}

-keep public class com.threerings.msoy.swiftly.client.SwiftlyApplet {
    *;
}
