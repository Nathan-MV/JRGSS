package org.jrgss;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl.audio.Ogg;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;
import com.badlogic.gdx.backends.lwjgl.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.jrgss.JRGSSApplication;
import org.jrgss.JRGSSApplicationListener;
import org.jrgss.JRGSSLogger;
import static org.jrgss.JRGSSLogger.LogLevels.*;
import org.jrgss.api.*;
import org.jrgss.api.Graphics;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.*;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.SharedDrawable;

import javax.swing.*;
import java.awt.*;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author matt
 * @date 7/5/14
 */
@Data()
public class JRGSSDesktop extends LwjglApplication implements JRGSSApplication{

    JRGSSApplicationListener jrgssApplicationListener;

    public JRGSSDesktop(JRGSSApplicationListener listener, String title, int width, int height) {
        this(listener, createConfig(title, width, height));
    }

    public JRGSSDesktop(JRGSSApplicationListener listener) {
        this(listener, null, 640, 480);
    }

    public JRGSSDesktop(JRGSSApplicationListener listener, LwjglApplicationConfiguration config) {
        super(listener,config);
        this.jrgssApplicationListener = listener;
        JRGSSLogger.println(DEBUG,"Seting up Application Window");
        audio.registerMusic("ogx", Ogg.Music.class);
        audio.registerSound("ogx", Ogg.Sound.class);
        audioUpdateThread.setDaemon(true);
        audioUpdateThread.start();
        // this(listener,
        //     config,
        //     new LwjglGraphics(config)
        //     );
        jrgssApplicationListener.loadScripts();
    }

    // public JRGSSDesktop(JRGSSApplicationListener listener, Canvas canvas) {
    //     this(listener,
    //         new LwjglApplicationConfiguration(),
    //         new LwjglGraphics(canvas)
    //         );
    // }

    public JRGSSDesktop(JRGSSApplicationListener listener, LwjglApplicationConfiguration config, LwjglGraphics graphics) {
        super(listener, config, graphics);
        this.jrgssApplicationListener = listener;
    }

    // public JRGSSDesktop(JRGSSApplicationListener listener, LwjglApplicationConfiguration config, Canvas canvas) {
    //     this(listener, config, new LwjglGraphics(canvas, config));
    // }


    private static LwjglApplicationConfiguration createConfig(String title, int width, int height) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = title;
        config.width = width;
        config.height = height;
        config.vSyncEnabled = true;
        return config;
    }

    volatile boolean killAudio = false;
    Thread audioUpdateThread = new Thread(new Runnable() {
        @Override
        public void run() {
            JRGSSLogger.println(DEBUG,"Starting Audio Thread");
            while(!killAudio) {
                JRGSSLogger.println(PEDANTIC,"Audio Loop");
                if (audio != null) {
                    synchronized (org.jrgss.api.JAudio.class) {
                        audio.update();
                    }
                }
                try {
                    Thread.sleep(16);
                }catch (Exception e){
                    JRGSSLogger.println(PEDANTIC,"Audio Loop ERROR");
                }
            }
            JRGSSLogger.println(DEBUG,"Exiting Audio Thread");
        }
    });

    // @Override
    void mainLoop() {
        JRGSSLogger.println(DEBUG,"Setting Up Main Loop");

        Array<LifecycleListener> lifecycleListeners = this.lifecycleListeners;
        while(jrgssApplicationListener == null) {
            try{
                Thread.sleep(1);
            }catch (InterruptedException e){}
        }
        // jrgssApplicationListener.loadScripts();
        try {
            // graphics.setupDisplay();
            Graphics.init();
        } catch (Exception e) {
            JRGSSLogger.printBuffer();
            throw new GdxRuntimeException(e);
        }

        // audio.registerMusic("ogx", Ogg.Music.class);
        // audio.registerSound("ogx", Ogg.Sound.class);
        listener.create();
        // graphics.resize = true;

        lastWidth = graphics.getWidth();
        lastHeight = graphics.getHeight();

        wasActive = true;
        // audioUpdateThread.setDaemon(true);
        // audioUpdateThread.start();
        try{
            JRGSSLogger.println(DEBUG,"Executing Main Loop");
            jrgssApplicationListener.getMain().main();
        }catch(Exception e) {
            JRGSSLogger.printBuffer();
            e.printStackTrace(System.err);
            JOptionPane.showMessageDialog(Display.getParent(), "Unexpected Error: "+e.getMessage(), "Error",JOptionPane.ERROR_MESSAGE);
        }

        JRGSSLogger.println(DEBUG,"Closing Down Display Window");
        synchronized (lifecycleListeners) {
            for (LifecycleListener listener : lifecycleListeners) {
                listener.pause();
                listener.dispose();
            }
        }
        killAudio = true;
        while (audioUpdateThread.isAlive()) {
            try{
                Thread.sleep(10);
            }catch (Exception e) {}
        }
        listener.pause();
        listener.dispose();
        Display.destroy();
        if (audio != null) audio.dispose();
    }

    int lastWidth;
    int lastHeight;
    boolean wasActive;

    @Override
    public void exit() {
        JRGSSLogger.println(DEBUG,"Display Exit Function Called - closing down");
        synchronized (lifecycleListeners) {
            for (LifecycleListener listener : lifecycleListeners) {
                listener.pause();
                listener.dispose();
            }
        }
        listener.pause();
        listener.dispose();
        Display.destroy();
        if (audio != null) audio.dispose();
        System.exit(0);
    }

    @Override
    public void handlePlatform() {
        JRGSSLogger.println(PEDANTIC,"Platform specific code being run");
        Display.processMessages();
        if (Display.isCloseRequested()) exit();

        boolean isActive = Display.isActive();
        if (wasActive && !isActive) { // if it's just recently minimized from active state
            JRGSSLogger.println(DEBUG,"Window Minimized - Pausing All Operations");
            wasActive = false;
            synchronized (lifecycleListeners) {
                for (LifecycleListener listener : lifecycleListeners)
                    listener.pause();
            }
            listener.pause();
        }
        if (!wasActive && isActive) { // if it's just recently focused from minimized state
            JRGSSLogger.println(DEBUG,"Window Being Displayed - Unpausing all Operations");
            wasActive = true;
            listener.resume();
            synchronized (lifecycleListeners) {
                for (LifecycleListener listener : lifecycleListeners)
                    listener.resume();
            }
        }
        // hideMouse();
        // boolean shouldRender = false;

        // if (graphics.canvas != null) {
        //     int width = graphics.canvas.getWidth();
        //     int height = graphics.canvas.getHeight();
        //     if (lastWidth != width || lastHeight != height) {
        //         JRGSSLogger.println(DEBUG,"Resizing Canvas : w"+lastWidth+" : h"+lastHeight);
        //         lastWidth = width;
        //         lastHeight = height;
        //         Gdx.gl.glViewport(0, 0, lastWidth, lastHeight);
        //         listener.resize(lastWidth, lastHeight);
        //         shouldRender = true;
        //     }
        // } else{
            // graphics.config.x = Display.getX();
            // graphics.config.y = Display.getY();
            // if (graphics.resize || Display.wasResized() || Display.getWidth() != graphics.config.width
            //         || Display.getHeight() != graphics.config.height) {
            //     graphics.resize = false;
                // Gdx.gl.glViewport(0, 0, Display.getWidth(), Display.getHeight());
                // graphics.config.width = Display.getWidth();
                // graphics.config.height = Display.getHeight();
                // if (listener != null) listener.resize(Display.getWidth(), Display.getHeight());
                // JRGSSLogger.println(DEBUG,"Resizing Graphics : w"+graphics.config.width+" : h"+graphics.config.height);
                // JRGSSLogger.println(DEBUG,"Resizing Graphics : w"+Display.getWidth()+" : h"+Display.getHeight());
                graphics.requestRendering();
            // }
        // }

        // if (executeRunnables()) shouldRender = true;
        executeRunnables();


        input.update();
        // shouldRender |= graphics.shouldRender();
        // input.processEvents();
        //if (audio != null) audio.update();

        // if (!isActive && graphics.config.backgroundFPS == -1) shouldRender = false;
        // int frameRate = isActive ? graphics.config.foregroundFPS : graphics.config.backgroundFPS;
        // if (shouldRender) {
        //     graphics.updateTime();
        listener.render();
        //     Display.update(false);
        // } else {
        //     // Sleeps to avoid wasting CPU in an empty loop.
        //     if (frameRate == -1) frameRate = 10;
        //     if (frameRate == 0) frameRate = graphics.config.backgroundFPS;
        //     if (frameRate == 0) frameRate = 30;
        // }
        // if (frameRate > 0) Display.sync(frameRate);
        Display.update(true);

    }

    @Override
    public boolean isFocused() {
        return Display.isActive();
    }

    @Override
    public void releaseContext() {
        try {
            Display.releaseContext();
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void acquireContext() {
        try {
            Display.makeCurrent();
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private org.lwjgl.input.Cursor emptyCursor;

    private void hideMouse() {

        try {
            if (emptyCursor == null) {
                if (Mouse.isCreated()) {
                    int min = org.lwjgl.input.Cursor.getMinCursorSize();
                    IntBuffer tmp = BufferUtils.createIntBuffer(min * min);
                    emptyCursor = new org.lwjgl.input.Cursor(min, min, min / 2, min / 2, 1, tmp, null);
                } else {
                    throw new LWJGLException(
                            "Could not create empty cursor before Mouse object is created");
                }
            }
            if (Mouse.isInsideWindow() && Display.isActive());
                Mouse.setNativeCursor(emptyCursor);
        } catch (LWJGLException e) {
            //We'll ignore this for now...
        }
    }

    public boolean equals(Object other) {
        return this == other;
    }

    public int hashCode() {
        return System.identityHashCode(this);
    }

}