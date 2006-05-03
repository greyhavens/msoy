This document contains a couple of notes about some design decisions
and some notes about flash that you may find useful.

TODO
----
- Write code that processes a dobj class in java and outputs the
  corresponding class in actionscript. This is sorta fucked because
  we want to exclude things not applicable to client code, not because
  we're trying to save every byte in the class definition, but because
  some of those methods involve whole classes we don't need on the as client.
- Write code that generates actionscript service, listener and marshaller
  classes from a java Service class definition.


Design decisions
----------------
- I have kept accessors named like their Java counterparts, rather
  than embracing flash's property setter/getter methods (which are really
  cool), but I am starting to lean the other way and may rewrite some stuff.

- I am embracing flash's event distribution model because it saved me a bunch
  of work.

- We could use the setter methods on DObject properties to generate dobj
  events, but so far I haven't gone there.

- We need a realistic HashMap implementation. Using Object properties
  (a-la my SimpleMap) is not going to cut it because keys must always
  be Strings.<strike>and there's no way to *really* remove a value from
  an Object (you can set the property to null, but now the property is
  forever defined: the key is not cleared)</strike>
  ***Update: the 'delete' operator removes properties.

  mx.utils.UIDUtil.getUID() can be used to generate a (huge) unique String
  for any object for use as a key or something.

  It might be worth waiting, I think it's very probable that Adobe will
  add in a Hashtable class to the standard libraries...

- Since we cannot do streaming via reflection like we do in Java, each
  Streamable class needs to define its own readObject/writeObject methods.
  At one point we thought that maybe we could just write the class
  and have a script examine the class definition and automatically generate
  those two methods, but I don't think that's going to save us much.
  Variables cannot be marked as transient, and we often have to change the
  type locally: actionscript has Number and int which correspond to
  float/double and int/short/byte when we stream to the server. So we'd have
  to do a bunch of crazy comment annotations on each variable to be streamed
  in any class and at that point we may as well just write the streamable
  methods, IMO.


Notes
-----
- In actionscript, 'package' is simply a block command to sweep whatever is
  defined inside the block so that it's in that package. This means
  that in addition to classes being in a package, freestanding functions
  and I believe variables and constants can be in a package.

  We are not putting freestanding functions anywhere. Make a util class with
  static methods.

- ActionScript does not have inner classes. Only one public class may be
  defined in a file, and the filename must match the public class.
  However, protected classes cannot be defined within the package block!
  So it seems like the model is:

  package com.foo {
      public class FooBar {
          // stuff
      }
  } // end: package foo

  class HelperClass {
      // helper stuff
  }


  To me, this makes it seem as if the helper class is now globally scoped,
  which of course is the exact opposite of what is desired. This may
  not be the case, I haven't played with it much yet.

  What especially sucks is that any imports must be repeated down below
  for the helper class, including importing the class defined just above.
  Again, it's unclear to me whether those imports are now globally scoped
  and will spill over onto other files... What a giant pain.

  ***Update: it turns out that the primary class in a file may be declared
  with internal accessibility. So HelperClass could live in its own file
  and access 'internal' methods on the main class. That is probably
  preferable to having them in the same file but having to re-import anyway
  and accessing only public properties of the main class from the helper.


- Sandboxing classes is done with ApplicationDomains. When we load a sub-swf
  we'll want to put it into a different domain so that nothing malicious
  can be done to our classes.

- <strike>constructors do not defaultly call super()- be sure to do it explicitely.
  Maybe we should get in the habit of doing it in Java for consistency and
  explicitness.</strike>
  CORRECTION: super() is called implicitely, just as in Java.

- It's annoying how there can be only one constructor: if you have classA
  that has a 1-arg constructor and it is extended by classB, then the implicit
  super() is inserted, but this results in runtime error because the classA
  constructor is not being passed an arg. You'd think this would be caught
  at compile time...

- The RENDER Event is dispatched prior to each rendering, it's
  basically like tick(): it gives anything that cares a chance to update    
  prior to being painted. It doesn't specify what the hell to listen on for
  this event, but since all DisplayObjects are event dispatchers then
  listening on any display object (including the stage) should work...

  But, the damn thing doesn't get dispatched if there will be no render,    
  even if the code is still running- like when the flash player window is   
  minimized or obscured. Lovely.

  I will play around with trying to just use a Timer with a 1ms interval,
  and see if the frequency is limited to the actual framerate.

  ***Update: The ENTER_FRAME event behaves like what I expected from RENDER.

- All methods must be marked with the 'override' keyword if they override
  a method in their parent, except for toString(), even though it's defined
  for Object. Apparently those methods are 'magic' and are not really in
  the base class. What an annoying inconsistency.

- 'protected' means something slightly different from java: other classes
   in the same package cannot access protected members, only subclasses may:

Java            Class Package Subclass World
  private       Y     N       N        N
  <default>     Y     Y       N        N
  protected     Y     Y       Y        N
  public        Y     Y       Y        Y

ActionScript  
  private       Y     N       N        N
  internal      Y     Y       N        N
  protected     Y     N       Y        N
  public        Y     Y       Y        Y


- Beware of non-existant integer math:

  var i :int = 3;
  var o :Object = someArray[i / 2];
  // o is now undefined, because we accessed array element "1.5".
  // I think arrays are just hashes, so probably you could store
  // values at element 1.5 if you desired...

- Similarly, methods in String take Number arguments (wha?) for character
  index positions. Totally nonsensical.

- Hey! Array has two constructors! How can I do that?
  - Probably they have one constructor with varargs, and it simply checks
  to see if there is only 1 arg and if it's an int, and then does something
  different. Although, we can't really be sure, because these classes are
  magic and special and don't have a corresponding .as file we can check out.

- I've been casting using 'as':
     var s :String = (someObject as String);

  But I've learned that there's another way that didn't seem to be listed
  anywhere in the language reference but is more like what we'll want:
     var s :String = String(someObject);

  The difference is that the first one tries to coerce the value to be
  of the specified type, and if it fails returns null. The second is
  more like a cast in Java, in that if it fails it generates an Error at
  runtime.

  Note that if the types are coercable, each one will succeed in the same way:
     var o :Object = 2.5; // create a Number object
     var x :int = (o as int);
     var y :int = int(o);
     // both of these work and turn the Number 2.5 into int 2.

  Perhaps we'll want a util method that always generates an error if the
  object's type is not identical or a subclass of the casted-to type.

  ***Update:
     var o1 :String = null;
     var o2 :String = String(o1); // ends up being "" or something

     The 2nd kind of cast destroys null, at least for String. So fuck that,
     I was trying to use it when pulling a value out of a hash, but if it
     wasn't there it got booched.

  ***Update:
     Just not casting is "implicit casting" and will result in a type error
     at runtime. This may be what we want, but it's maddening that there's
     no way to do it explicitely.

     var o1 :String = someObject;
     // checked at runtime, throws TypeError if failure

     Also, when the compiler is in strict mode it flags this code, so
     we can't win.

   I will sum up in a table:

   * cast using "obj as Type"
     + helps compile-time type checking
     - turns non-castable objects into null rather than generating an exception
   * casting using "Type(obj)"
     + helps compile-time type checking
     - will coerce primitive types between each other, the most annoying
       problem being:
       var o1 :Object = null;
       var s1 :String = String(o1);
       assert(s1 === "null");
   * implicit casting ("var s :String = o")
     + it will generate a proper TypeError at runtime
     - no compile-time checking, strict compiler generates an error (!!!)
     

- Pitfall! This is perfectly legal:
     var b :int = 3;
     var b :int = 4;

  This will generate a compile warning:
     var b :int = 3;
     var b :String = "three";
  It generates the warning on assigning 3 to b, because it has looked
  into the future and decided that b is a String, even though it's an
  int on that line.

  And:

  var b :int = 3;
  for (var ii:int = 0; ii < b; ii++) {
      var b :Number = 3.3;
  }
  trace(b); // prints "3.3", even though we've left the loop


- AS3.0 allows for a bit of introspection, using the function
  flash.util.describeType(). The only problem is that if you pass in a Class
  then it always says that it's final (I guess it's the class's Class). It
  will dump information identical to the information given about an instance
  except that the dynamic/final information is lost. This is preventing
  me from correctly streaming arrays, as we need to know if the class
  is final. I can't just pass an instance in because it may be a pain
  to construct, it may even be unconstructable if the type of the array
  is an interface. Posted as a request for enhancement on the AS3.0 forums.


- Actionscript's property accessors are a cool feature, but beware hidden
  performance issues: accessing a simple property of a variable
  (like myArray.length) may actually be executing arbitrary code, possibly
  creating many objects, each time.

- <strike>Classes without a constructor cannot be instantiated. This is a
  runtime error (grraah!).</strike>
  There is a compiler option "-compiler.warn-no-constructor"
  but it generates a flotilla of warnings from standard classes in the flash
  library, so it's slightly useless.
  ***Update: What the heck. I noticed today that DSet has no constructor
  and I've never had any trouble instantiating those. Why would not having
  a constructor be an error for some classes and not others? Grraahh!
  ***Update: The language spec says that a default (no arg) constructor
  is created implicitely if one is not defined. Probably when I got
  the error that a class had no constructor it was some sort of compiler
  bug.

- Static initializers can be emulated:
  public class A
  {
      private static function staticInit () :void
      {
          // whatever
      }

      staticInit(); // will be placed inside the real static initializer
  }

- Unlike in Java, most operators are overloaded for strings:
      if (str1 > str2) {  // compares asciibetically

- It's pissing me off that some classes magically can use array dereferencing
  ([]) to do magical things, but there is no clear indication of which classes
  support it and which don't: you just have to scan through the class
  documentation. Array itself is dynamic, supposedly they needed to do that
  to store things in it, but it shouldn't be used as a dynamic class. Some
  of the collection-type classes also support []ing as does the arguments
  class. Those aren't dynamic though: they're just magical, and as far
  as I can tell there's no way to grant this magic to my own classes.
  I'd feel better about it if there were some marker interface implemented
  by all classes that can be []'d.

- Functions may be declared anywhere, and it seems that they have visibility
  to any variables around them at that point, as if they were an inner class
  and the variables were final:

  var list :ArrayCollection = new ArrayCollection();
  list.addItem(foo);

  var funcy :Function = function (i :int) :void {
     Log.debug("I can see " + list[0]);
  };
  _savedFunc = funcy;

  Then _savedFunc can be called at any later date and it can access list[0]
  just fine.

  This just might save our butts from insane class proliferation with service
  listeners.

- You can't use runtime constants as parameter initializers:

  public const MAX_VALUE :int = 99;

  // this is illegal because MAX_VALUE is not defined until the
  // static initializer is run for this class. It's not around at compile time.
  public function getCrap (minValue :int, maxValue :int = MAX_VALUE) :Crap

- Static constants are not inherited by subclasses. You can make them
  prototype rather than static and they will be.

- anonymous class options:

  - pass arrays of functions, with just a convention as to which function is
    which (no compile-time type checking)
  - pass objects with functions of the right names attached (no compile-time)
  - make adapters, as necessary, for interfaces (bleah!) (Still no good
    compile-time checking, except for the # of args)
  - add code to verify the object's functions against describeType calls..
    (would need to iterate on types because describeType only finds methods
    in the terminal interface. Only # of args can be checked)

- Private constructors are not allowed, so the official line from Macromedia
  on creating Singleton classes, I-shit-you-not, is to do this:

package foo {

public class Singleton
{
    public static const singleton :Singleton = new Singleton(new SecretClass());

    public function Singleton (secret :SecretClass)
    {
    }
}
} // end: package

public class SecretClass // inaccessible outside this file
{
}



- We can communicate easily with user-created AVM1/AVM2 swfs with no API
  integration, however, there is no way to identify the connection
  as belonging to a particular loaded swf.

In other words, if two avatars are sitting on the couch, one is Bob and one
is Jim, and they're each represented with the same media, I need to have
that media know, internally, that it is either Bob or Jim, so that
it can open a LocalConnection server to listen on a connection called
_Avatar=Bob, or something. Then we can have the msoy app send a
setLook("facingLeft") to the Bob avatar.

What we can do is specify to load the swf with ?oid=<foo> on the URL. Then
the user swf can do:
    var connName :String = this.root.loaderInfo.parameters["oid"];
    var lc :LocalConnection = new LocalConnection();
    lc.client = new Object();
    lc.client.setStyle = funcRef;
    lc.connect(connName); 

    // and then the msoy app can send style changes on a localconnection
    // by specifying the oid.

    downsides:
    - url parameter defeats caching
    - there's no way to ensure a user .swf complies and doesn't listen on
      other connection names or even send signals to other connection names.
      Indeed, you could just write something to try every numeric oid,
      constantly, and try to fuck up other avatar .swfs.

==> So maybe we just go with different swfs for different "looks".


- That of course brings up the issue of a malicious swf. There's nothing we
can do to prevent a user from including "while (true) {}" somewhere in
their code, and maybe only activating it in certain circumstances. The
flash player is essentially single-threaded, and doing this seems to lock
up the entire browser in my firefox, it's not even limited to the hosted tab.

Normally, nobody would use a malicious swf, but maybe some vandal kid
hacks his flash player to ignore his malicious code and then he sets his
avatar as that .swf and wanders around, freezing up the browsers of
all other clients every time he enters a room.

Even excluding that case, there's nothing we can do to prevent a malicious .swf
freezing shit up in the catalog viewer. More thinking is required.

- Duh. When b3 comes out and msoy compiles again, I should set breakpoints
  during the chatobj subscription process and see if I can narrow down
  which code is causing Mr. Crashypoo.

- Flex data services:
  LOOK INTO: is it possible to create a RemoteObject that is defined on
  the fly? (Without being specified in flex-enterprise-services.xml)
  (Or some other way to accomplish the same thing)
  Probably this is possible.

- describeType(someObject) and see about setting accessors dynamically,
  registerClass(), or other undocumented functions.

- We can probably use APT (Java 5's Annotation Processing Tool) to generate
.as files based on any annotations and other class information available
via reflection. Processors can be plugged in to generate new files.

http://java.sun.com/j2se/1.5.0/docs/guide/apt/GettingStarted.html

If it doesn't work out we could also use XDoclet2, which already has a
supported plugin for generating actionscript 3 classes from java classes.
It might be a tiny bit more work to use APT, but it's a more standard tool
and would require less setup for other developers, and I kinda figure
XDoclet2 is a dead-end now that Java5 has annotations and APT.

- Any display object can be rendered into a bitmap using BitmapData.draw().
http://kuwamoto.org/2006/05/02/how-to-make-a-flex-effect-explode-v-05/

- Security.allowDomain() is used on a swf to allow other swfs to "script" it.
We may need to have user-submitted swfs do this to allow advanced signalling
to a swf. Or, if we serve all swfs from the same machine, we may need to look
into doing some namespace business to prevent hackery (listening for passwords,
or being able to arbitrarily examine variables, or pop up dialogs that look
like msoy dialogs).

- OpenLaszlo is something to consider, even if we don't end up using any of it,
  it's a good resource for up-n-coming devkiddies.
