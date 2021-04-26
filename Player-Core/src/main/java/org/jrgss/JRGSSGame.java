package org.jrgss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import org.jrgss.api.*;
import org.jrgss.api.Graphics;
import org.jrgss.api.win32.User32;
import org.jrgss.rgssa.EncryptedArchive;
import org.jrgss.JRGSSLogger;
import static org.jrgss.JRGSSLogger.LogLevels.*;
import org.jruby.*;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.JarURLConnection;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by mcanterb on 6/26/14.
 */
public class JRGSSGame implements JRGSSApplicationListener {
    ScriptingContainer scriptingContainer;
    //my problem might be that some of these classes are not modules
    final String[] BUILTINS = new String[] {
            "JAudio", "Bitmap", "Graphics",
            "Plane", "Rect", "RGSSError", "Sprite",
            "Tilemap", "Tone", "Viewport", "Window",
            "RGSSReset"
    };

    public static final Queue<FutureTask<?>> glRunnables = new ConcurrentLinkedQueue<>();

    static JRGSSMain mainBlock;
    SpriteBatch batch;
    public static OrthographicCamera camera;
    FPSLogger fpsLogger;
    static Thread glThread;
    static ConfigReader ini;

    public static boolean isRenderThread() {
        return Thread.currentThread() == glThread;
    }

    public JRGSSGame(String gameDirectory, String rtpDirectory, ConfigReader ini) {
        super();
        JRGSSLogger.println(DEBUG,"Creating Game Engine Object");
        JRGSSGame.ini = ini; //Pretty shitty, but JRGSSGame should be a singleton
        FileUtil.setLocalDirectory(ini.getTitle());
        RGSSVersion rgss = ini.getRGSSVersion();
        if(rgss != RGSSVersion.VXAce) {
            JRGSSLogger.println(ERROR,"RPGM Version of the game files is not equal to VXAce - issue maybe occur");
            int result = JOptionPane.showConfirmDialog(null, "This game uses an unsupported version of RGSS. This game is for "+
            rgss+" There may be some incompatibilities!",
            "Unsupported RGSS Version", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if(result == JOptionPane.CANCEL_OPTION) {
                System.exit(0);
            }
        }
        FileUtil.setGameDirectory(gameDirectory);
        FileUtil.setRTPDirectory(rtpDirectory);
        File encryptedArchiveFile = new File(gameDirectory+File.separator+"Game.rgss3a");
        if(encryptedArchiveFile.exists()) {
            try{
                EncryptedArchive archive = new EncryptedArchive(gameDirectory+File.separator+"Game.rgss3a");
                FileUtil.setEncryptedArchive(archive);
            }catch (Exception e) {
                System.err.println("Could not load archive!");
                e.printStackTrace(System.err);
            }
        }else{
            JRGSSLogger.println(INFO,"Unable to find encrypted archive file Game.rgss3a - assuming the files are loose in a directory");
        }
    }

    public void loadScriptsFromDirectory(String path) {
        JRGSSLogger.println(INFO,"Loading Scripts in directory : "+path);
        File[] fileList = new File(path).listFiles();
        File[] orderedList = new File[fileList.length];
        for(File f : fileList) {
            String fileName = f.getName();
            JRGSSLogger.println(PEDANTIC,"FILENAME : "+fileName);
            String[] nameParts = fileName.split("@@__@@");
            orderedList[Integer.parseInt(nameParts[0])] = f;
        }

        JRGSSLogger.println(PEDANTIC,"Loading Previously Found Scripts into JRuby");
        for(File f : orderedList) {
            try{
                JRGSSLogger.println(PEDANTIC,"FILENAME : "+f.getName());
                scriptingContainer.runScriptlet(new FileReader(f),f.getName().split("@@__@@")[1]);
            }catch(Exception e) {
                JRGSSLogger.printBuffer();
                throw new RuntimeException(e);
            }
        }
    }

    //I think this is supposed to load a script from a rvdata2 file
    public void loadScriptData(String path) {
        JRGSSLogger.println(INFO,"Loading Scripts From Data File : "+path);
        FileHandle f;
        if(FileUtil.archive != null) {
            JRGSSLogger.println(DEBUG,"Opening file from compressed archive");
            f = FileUtil.archive.openFile(path);
        } else {
            JRGSSLogger.println(DEBUG,"Opening the loose file from the game directory, not archive");
            JRGSSLogger.println(PEDANTIC,"Script data file path separator : "+File.separator);
            if(!File.separator.equals("\\")) { // seems like a workaround for making these utils work on non-windows systems with the paths interanlly using windows style paths
                path = path.replaceAll("\\\\", File.separator);
            }
            f = new FileHandle(FileUtil.gameDirectory + File.separator + path);
        }

        //Read the whole file and let Ruby parse it out. It is an array of content.
        byte[] bytes = f.readBytes();
        RubyString str = RubyString.newString(Ruby.getGlobalRuntime(), bytes);
        RubyArray arr = (RubyArray)RubyMarshal.load(ThreadContext.newContext(Ruby.getGlobalRuntime()),
                null,
                new RubyObject[]{str},
                null);
        scriptingContainer.put("$RGSS_SCRIPTS", arr); // store the array in Ruby under the variable name $RGSS_SCRIPTS

        //We are now going to go through every index in the array and load the script data into Ruby
        //each script is compressed so we will decompress it
        //not every index will have content, but empty data should be safe to decompress and ask Ruby to evaluate
        for(Object o : arr) {
            RubyArray item = (RubyArray)o;
            String name = (String)item.get(1);
            JRGSSLogger.println(DEBUG,"    "+name);
            scriptingContainer.put("$__obj", item); // temp variable to decompress with
            String str2 = (String)scriptingContainer.runScriptlet("Zlib::Inflate.inflate($__obj[2]).force_encoding(\"utf-8\")");
            JRGSSLogger.println(PEDANTIC,str2); // MUHAHHA - You get ALL the scripts dumped to console if you really want to be pedantic
            try{
                String script = "# encoding: UTF-8\n"+str2;//.replaceAll("\r\n","\n");
                scriptingContainer.setScriptFilename(name);
                scriptingContainer.runScriptlet(script);
            }catch (Exception e) {
                throw new RuntimeException(e);
            }
            scriptingContainer.put("$__obj", null); // removing the variable since we are done decompressing
        }
    }

    //This loads the .rb files from the resources/rpg/ folder and throws it into the Ruby runtime
    public void loadRPGModule() {
        try{
            String[] files = new File(JRGSSGame.class.getResource("/rpg/").toURI()).list();
            JRGSSLogger.println(INFO,"Loading Ruby Files");
            for(String file : files) {
                JRGSSLogger.println(DEBUG,"    "+file);
                InputStream stream = JRGSSGame.class.getResourceAsStream("/rpg/"+file);
                scriptingContainer.runScriptlet(stream, file);
            }
        } catch(Exception e) {
            //We might be in a jar.
            try {
                String jarFileLocation = JRGSSGame.class.getProtectionDomain().getCodeSource().getLocation().getFile();
                jarFileLocation = jarFileLocation.replace("%20", " ");
                JarFile file = new JarFile(jarFileLocation);
                Enumeration<JarEntry> entries = file.entries();
                JRGSSLogger.println(INFO,"Loading Ruby Embedded Files");
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith("rpg")) {
                        JRGSSLogger.println(DEBUG,"    "+entry.getName());
                        scriptingContainer.runScriptlet(file.getInputStream(entry), entry.getName());
                    }
                }
            }catch(Exception e1) {
                throw new RuntimeException("Failed to load built in ruby scripts!", e1);
            }
        }
    }

    public static void jrgssMain(JRGSSMain rubyBlock) {
        mainBlock = rubyBlock;
    }

    //Load the Basic Engine classes that all games use
    //These are implemented as Java classes that JRuby pretends are Ruby classes
    //Perhaps there is something we can do to make it import as modules instead of classes?
    public void loadRGSSModule(String name) {
        JRGSSLogger.println(DEBUG,"Embedding Java Class As Ruby Class : "+name);
        scriptingContainer.runScriptlet("java_import org.jrgss.api."+name);
        scriptingContainer.runScriptlet("class "+name+"\ndef _dump level\nself.dump\nend\nend");
    }



    @Override
    public void create() {
        JRGSSLogger.println(DEBUG,"Creating a drawing context");
        if(SplashScreen.getSplashScreen()!=null) {
            SplashScreen.getSplashScreen().close();
        }
        glThread = Thread.currentThread();

        camera = new OrthographicCamera(Graphics.getWidth(), Graphics.getHeight());
        camera.setToOrtho(true, Graphics.getWidth(), Graphics.getHeight());
        camera.update();
        fpsLogger = new FPSLogger();
        Gdx.graphics.setVSync(true);
        batch = new SpriteBatch();
        batch.enableBlending();
    }

    @Override
    public void resize(int width, int height) {
        JRGSSLogger.println(DEBUG,"Game Resize : w"+width+" : h"+height);
        camera = new OrthographicCamera(Graphics.getWidth(), Graphics.getHeight());
        camera.setToOrtho(true, Graphics.getWidth(), Graphics.getHeight());
        camera.update();
    }

    @Override
    public void render() {
        JRGSSLogger.println(PEDANTIC,"Game Render");
        update();
        batch.setProjectionMatrix(camera.combined);
        batch.setColor(1f,1f,1f,1f);
        //batch.begin();
        Graphics.render(batch);
        //batch.end();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {}

    public static void runWithGLContext(final Runnable runnable) {
        if(Thread.currentThread() == glThread) {
            JRGSSLogger.println(DEBUG,"GL Context Setup - Thread starting");
            runnable.run();
        } else {
            //FutureTask<?> task = new FutureTask<Object>(runnable, null);
            Gdx.app.postRunnable(runnable);
        }
    }

    DebugFrame debugFrame = new DebugFrame();
    boolean buttonTrigger = false;

    public void update() {
        if(!buttonTrigger && Gdx.input.isKeyPressed(Input.Keys.F1)) {

            debugFrame.refresh();
            buttonTrigger = true;
        }
        if(buttonTrigger && !Gdx.input.isKeyPressed(Input.Keys.F1)) {
            buttonTrigger = false;
        }
    }

    static boolean override = false;

    @Override
    public void loadScripts() {
        JRGSSLogger.println(INFO,"Loading Game Scripts, Please Wait...");
        scriptingContainer = new ScriptingContainer(LocalContextScope.SINGLETON, LocalVariableBehavior.PERSISTENT);
        scriptingContainer.setCompatVersion(CompatVersion.RUBY1_9);
        scriptingContainer.setCompileMode(RubyInstanceConfig.CompileMode.OFF);
        scriptingContainer.setRunRubyInProcess(true);
        //scriptingContainer.runScriptlet("$TEST=true");
        scriptingContainer.runScriptlet("require 'java'");
        scriptingContainer.runScriptlet("require 'org/jrgss/api/RGSSBuiltin'");
        for(String module: BUILTINS) {
            loadRGSSModule(module);
        }

        loadRPGModule(); // load the embedded .rb scripts in this project (shims and bootstrap)
        scriptingContainer.put("$_jrgss_home", FileUtil.gameDirectory);
        scriptingContainer.put("$_jrgss_paths", new String[]{FileUtil.localDirectory, FileUtil.gameDirectory});
        scriptingContainer.put("$_jrgss_os", System.getProperty("os.name"));
        loadScriptData(ini.getScripts());
        //Gdx.app.log("JRGSSGame", scriptingContainer.runScriptlet("load_data(\"Data/Map101.rvdata2\")").toString());
    }

    @Override
    public JRGSSMain getMain() {
        return mainBlock;
    }


    public static interface JRGSSMain {
        public void main();
    }


}
