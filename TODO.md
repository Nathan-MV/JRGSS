# TODO

* Proper printing framework to support the above log levels
	* This will also be the time where we remove some of the annoying prints that happens currently
* use case insensitive variable lookup in for the Game.ini file
	* we should not force using Library= when vxa outputs library= by default
* Use a thin shim ruby module layer that just directly calls the java methods
	* This will allow the ruby scripts to override and modify behavior
	* Not allowing the overriding will prevent the more extensive games that do more than just vanilla operations
	* Ideally this is not required and we can have JRuby allow dynamic rebinding of Java implementations, but this seems to be a limitation of that implementation

# Stretch Goals

* Main Menu that is blank and not running a game at all
	* It would be nice to have a window that has just a few tools available such as selecting a game to try running or simply extracting the data from the game archive
	* These options we would add could be served just as well by command line options, but it would be nice to make it easy for less technical people
* Integrated Debugger
	* --showDebuggerWindows command line option
	* Seperate windows being opened that show what line of code is being executed in Ruby scripts, along with a memory viewer/editor
	* Double stretch goal of the memory viewer having the vaiables and object names tracked, perhaps even pixel rendering for sprites/textures

# Known bugs

## Magical Camp 0.4.7b

* Main Menu - selection of options highlight box is in the wrong location
	* is on the top left out of the window frame

* after running the game once, this happens
	glop102@genServer /media/TerminalDogma/Executables/hentai games/MagicalCamp047b $ ./linux.sh 
	Using Case Insensitive File lookups!
	Running on Linux
	Key is 48e5
	Offset is 12
	Exception in thread "LWJGL Application" java.lang.ExceptionInInitializerError
		at com.badlogic.gdx.backends.lwjgl.LwjglGraphics.setVSync(LwjglGraphics.java:446)
		at com.badlogic.gdx.backends.lwjgl.LwjglApplication$1.run(LwjglApplication.java:118)
	Caused by: java.lang.NullPointerException
		at org.lwjgl.opengl.LinuxDisplay.getAvailableDisplayModes(LinuxDisplay.java:950)
		at org.lwjgl.opengl.LinuxDisplay.init(LinuxDisplay.java:738)
		at org.lwjgl.opengl.Display.<clinit>(Display.java:138)
		... 2 more
	AL lib: (EE) alc_cleanup: 1 device not closed
* need to clean up all audio channels when shutting down
* https://stackoverflow.com/questions/16161714/what-does-al-lib-alc-cleanup-1-device-not-closed-mean
* calling System.exit() does not let resources get cleaned up properly - maybe this is the problem? Perhaps we are simply never calling Gdx.app.exit()?