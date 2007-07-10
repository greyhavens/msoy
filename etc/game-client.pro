#
# $Id$
#
# Proguard configuration file for the Java game client

-injars ../dist/lib/commons-collections.jar(!META-INF/*)
-injars ../dist/lib/commons-io.jar(!META-INF/*)
-injars ../dist/lib/commons-digester.jar(!META-INF/*)
-injars ../dist/lib/samskivert.jar(
    com/samskivert/Log.class,**/io/**,**/net/**,**/swing/**,**/text/**,**/util/**,**/xml/**,
    **/servlet/user/Password.class,**/servlet/user/User.class,**/servlet/user/UserUtil.class)
-injars ../dist/lib/getdown.jar(!META-INF/*,!**/tools/**)
-injars ../dist/lib/narya-base.jar(!META-INF/*,!**/tools/**,!**/server/**,!**/admin/**)
-injars ../dist/lib/narya-distrib.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/nenya-rsrc.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/nenya-media.jar(!META-INF/*,!**/tools/**,!**/server/**)
-injars ../dist/lib/vilya-parlor.jar(!META-INF/*,!**/tools/**,!**/server/**,!**/xml/**)
-injars ../dist/lib/vilya-stats.jar(!META-INF/*,!**/tools/**,!**/persist/**)
-injars ../dist/lib/vilya-whirled.jar(**/ClusteredBodyObject.class,**/ScenedBodyObject.class)
-injars ../dist/lib/toybox.jar(!META-INF/*,!**/tools/**,!**/server/**,!**/xml/**)
-injars ../dist/lib/whirled.jar(!META-INF/*,!**/tools/**,!**/server/**,!**/xml/**)
-injars ../dist/lib/gwt-user.jar(**/user/client/rpc/IsSerializable.class)
-injars ../dist/msoy-code.jar(
    rsrc/i18n/**,**/msoy/Log.class,**/msoy/data/**,**/msoy/client/**,**/msoy/item/data/all/**,
    **/msoy/world/data/WorldMemberInfo.class,**/msoy/world/data/WorldOccupantInfo.class,
    **/msoy/notify/data/**,**/msoy/game/data/**,**/msoy/game/client/**)

-dontskipnonpubliclibraryclasses
-dontobfuscate
-outjars ../dist/game-client.jar

# we need whatever we keep of samskivert to be around in its entirety so
# that if a game uses the same classfile, the whole thing is there
-keep public class com.samskivert.Log {
    public protected *;
}
-keep public class com.samskivert.io.** {
    public protected *;
}
-keep public class com.samskivert.net.AttachableURLFactory {
    public protected *;
}
-keep public class com.samskivert.net.PathUtil {
    public protected *;
}
-keep public class com.samskivert.servlet.user.Password {
    public protected *;
}
-keep public class com.samskivert.servlet.user.UserUtil {
    public protected *;
}
-keep public class com.samskivert.swing.** {
    public protected *;
}
-keep public class com.samskivert.text.MessageUtil {
    public protected *;
}
-keep public class com.samskivert.util.** {
    public protected *;
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

# similarly for all of the narya, nenya and vilya libraries
-keep public class com.threerings.** {
    public protected *;
}

# similarly for all of the whirled libraries
-keep public class com.whirled.** {
    public protected *;
}

# we need to cope with an MSOY auth respose
-keep public class com.threerings.msoy.data.MsoyAuthResponseData
