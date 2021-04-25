package org.jrgss;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.JRGSSDesktop;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import org.lwjgl.input.Mouse;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Hashtable;
import static java.lang.System.out;
import java.lang.System;

/**
 * Created by mcanterb on 6/26/14.
 */
public class Desktop {
    static Hashtable<String,String> cliOptions = new Hashtable<String,String>(); // gets filled by the parseCLIArgs function

    public static void main(String[] args) {
        try {
            parseCLIArgs(args);
            LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();

            cfg.title = "Title";
            cfg.useGL30 = false;
            cfg.width = 800;
            cfg.height = 450;
            cfg.vSyncEnabled = true;
            cfg.stencil = 8;
            cfg.resizable = true;
            cfg.useHDPI = true;
            cfg.fullscreen = false;
            if(System.getProperty("jrgss.icon") != null && !System.getProperty("os.name").toLowerCase().contains("linux")) {
                cfg.addIcon(System.getProperty("jrgss.icon"), Files.FileType.Absolute);

            } else {
                FileUtil.onCaseSensitiveFileSystem = true;
                System.out.println("Using Case Insensitive File lookups!");
            }
            ConfigReader config = new ConfigReader(cliOptions.get("gameDirectory") + File.separator + "Game.ini");
            cfg.title = config.getTitle();
            setDockIconIfOnOSX(System.getProperty("jrgss.icon"));
            new JRGSSDesktop(new JRGSSGame(cliOptions.get("gameDirectory"), cliOptions.get("rtpDirectory"), config), cfg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Encountered an unexpected error: "+e.getMessage(),"Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void setDockIconIfOnOSX(String iconFile) {
        if(iconFile != null && System.getProperty("os.name").toLowerCase().contains("mac")) {
            try {
                Class<?> clazz = Class.forName("com.apple.eawt.Application");
                Method getApplication = clazz.getDeclaredMethod("getApplication");
                Method setDockIconImage = clazz.getDeclaredMethod("setDockIconImage", Image.class);
                Object application = getApplication.invoke(null);
                BufferedImage icon = ImageIO.read(new File(iconFile));
                setDockIconImage.invoke(application, icon);
            } catch (Exception e) {
                //Failed to set Dock Icon. Probably not on OSX
                System.err.println("Failed to set OS X Dock icon! ");
                e.printStackTrace(System.err);
            }
        }
    }


    //===================================================================================
    // CLI Parsing
    //===================================================================================
    public static class CliOption{
        final String longName;
        final String shortName;
        final String helpDescription;
        final String defaultValue;
        final boolean consumesNextArg;
        final boolean nextArgConsumedOptional; // example is the --verbose flag - you can give a number next or give an argument next at it assumes a default
        final String defaultValueIfSeen; // if this argument does not consume an argument, what value do we give it?
        String value;
        public CliOption(String a, String b, String c, String d, boolean e, boolean f, String g){
            longName=a; shortName=b; helpDescription=c; defaultValue=d; consumesNextArg=e; nextArgConsumedOptional=f; defaultValueIfSeen=g;
            value = defaultValue;
        }
    }
    static CliOption[] availableCliOptions = {
        new CliOption("gameDirectory","g","The directory of the game to run", ".", true, false, null),
        new CliOption("rtpDirectory","r","The directory of the rtp of the game. It defaults to the same directory as the game", null, true, false, null),
        new CliOption("verbose","v","turns on debug prints for the java code", "0", true, true, "1"),
        new CliOption("allowRubyPrints",null,"turns on prints coming from ruby code", "0", false, false, "1")
    };
    public static void parseCLIArgs(String[] args){

        Hashtable<String, CliOption> longOpts = new Hashtable<String, CliOption>();
        Hashtable<String, CliOption> shortOpts = new Hashtable<String, CliOption>();

        //lets get our lookup tables set up
        for (CliOption opt : availableCliOptions){
            longOpts.put(opt.longName,opt);
            if( opt.shortName != null )
                shortOpts.put(opt.shortName,opt);
        }

        for (int i=0; i<args.length; i++){
            //First find what argument we are trying to parse
            String arg = args[i];
            CliOption opt = null;
            if( arg.equals("-h") || arg.equals("--help") ){
                printHelp();
            } else if( arg.indexOf("--") == 0 ){
                arg = arg.substring(2);
                opt = longOpts.get(arg);
            }else if( arg.indexOf("-") == 0 ){
                arg = arg.substring(1);
                opt = shortOpts.get(arg);
            }
            if( opt == null ){
                out.println("Unknown Option \""+arg+'"');
                printHelp();
            }

            //now handle setting the value of the argument
            if( opt.consumesNextArg && opt.nextArgConsumedOptional )
                if( i+1<args.length && args[i+1].indexOf("-")!=0 ){
                    opt.value = args[i+1];
                    i++; // skip the consumed arg
                }else{
                    opt.value = opt.defaultValueIfSeen;
                }
            else if( opt.consumesNextArg && !opt.nextArgConsumedOptional )
                if( i+1<args.length ){
                    opt.value = args[i+1];
                    i++; // skip the consumed arg
                }else{
                    out.println("Missing argument for \""+arg+'"');
                    printHelp();
                }
            else
                opt.value = opt.defaultValueIfSeen;
        }

        //Now that we have parsed all the values out of the cli args, lets do some dependency cleanup
        { // defualt the rtpDirctory to the game directory if the user has not already given a location
            CliOption ropt = longOpts.get("rtpDirectory");
            if( ropt.value == null )
                ropt.value = longOpts.get("gameDirectory").value;
        }

        //we can put the final values into th hashtable
        for (CliOption opt : availableCliOptions){
            cliOptions.put(opt.longName,opt.value);
        }
    }

    public static void printHelp(){
        out.println("Available Options:");
        for(CliOption opt : availableCliOptions){
            if( opt.consumesNextArg && opt.nextArgConsumedOptional )
                out.println("\t--"+opt.longName+" -"+opt.shortName+" [argument] :");
            else if( opt.consumesNextArg && !opt.nextArgConsumedOptional )
                out.println("\t--"+opt.longName+" -"+opt.shortName+" <argument> :");
            else
                out.println("\t--"+opt.longName+" -"+opt.shortName+":");
            out.println("\t\t"+opt.helpDescription);
            if( opt.defaultValue != null )
                out.println("\t\tDefault Value : "+opt.defaultValue);
        }
        System.exit(-1);
    }
}
