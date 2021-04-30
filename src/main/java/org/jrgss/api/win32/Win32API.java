package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import lombok.ToString;
import org.ini4j.Wini;
import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.io.File;

import org.jrgss.JRGSSLogger;
import static org.jrgss.JRGSSLogger.LogLevels.*;

/**
 * @author matt
 * @date 8/20/14
 * This is a class to load functions out of the dll runtime.
 * Since we are trying to be platform agnostic, we have some hand-coded specific functions that are used instead.
 */
@ToString
@JRubyClass(name = "Win32API")
public class Win32API extends RubyObject{

    static public Ruby runtime;
    static public RubyClass rubyClass;
    static final HashMap<DLLEntry, DLLImpl> funcEntries = new HashMap<>();

    String dll;
    String func;
    String spec;
    String ret;

    private DLLImpl impl;

    public Win32API(final Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    @JRubyMethod(required = 4)
    public void initialize(ThreadContext context, IRubyObject[] args) {
        this.dll = args[0].asJavaString();
        this.func = args[1].asJavaString();
        this.ret = args[3].asJavaString();
        if(args[2] instanceof RubyArray) {
            StringBuilder builder = new StringBuilder();
            RubyArray specArray = args[2].convertToArray();
            for(IRubyObject rubyObject : specArray.toJavaArray()) {
                if(!(rubyObject instanceof RubyString)) {
                    throw new IllegalArgumentException("Arguments in function spec array must be Strings! Was "+rubyObject.getClass());
                }
                builder.append(rubyObject.asJavaString());
            }
            this.spec = builder.toString();
        } else if(args[2] instanceof RubyString) {
            this.spec = args[2].asJavaString();
        }
        DLLImpl m = funcEntries.get(new DLLEntry(dll, func, spec));
        if(m == null) {
            JRGSSLogger.println(DEBUG,"DLL Function not found - Returning stub for "+this.toString());
            m = STUB_METHOD;
        }
        impl = m;
    }


    @JRubyMethod(name = "call", alias = "Call", optional = 16)
    public IRubyObject call(ThreadContext context, IRubyObject[] args) {
        if(impl == STUB_METHOD){
            switch(this.func){
                case "GetPrivateProfileInt":
                    return GetPrivateProfileInt(context,args);
                case "WritePrivateProfileString":
                    return WritePrivateProfileString(context,args);
                default:
                    JRGSSLogger.println(ERROR,"Stub Called for "+this.toString());
            }
        }
        return impl.call(this, context, args);
    }




    static final DLLImpl STUB_METHOD = new DLLImpl() {
        @Override
        public IRubyObject call(Win32API api, ThreadContext context, IRubyObject[] args) {
            if(api.ret.equals("i")) {
                return new RubyFixnum(context.runtime, 0);
            }
            return context.runtime.getNil();
        }
    };

    public static void registerWin32Functions(Class<?> clazz) {
        try {
            for(Field f : clazz.getDeclaredFields()) {
                Win32Function functionMeta = f.getAnnotation(Win32Function.class);
                if(functionMeta == null) continue;
                Object val = f.get(null);
                if(val instanceof DLLImpl) {
                    DLLEntry entry = new DLLEntry(functionMeta.dll(), functionMeta.name(), functionMeta.spec());
                    funcEntries.put(entry, (DLLImpl)val);
                } else {
                    Gdx.app.log("Win32API", f.getName()+" in "+clazz.getName()+" is not a DLLImpl but is annotated with @Win32Function");
                }
            }
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //TODO - This only supports parsing out values as ints. We need to preserve the argument type for the DLL load and parse the right object type out.
    public IRubyObject GetPrivateProfileInt(ThreadContext context, IRubyObject[] args){

        String section = args[0].asJavaString();
        String key = args[1].asJavaString();
        int defaultVal = args[2].toJava(int.class);
        String filename = args[3].asJavaString();
        String out = null;
        JRGSSLogger.println(PEDANTIC,"Internal GetPrivateProfileInt() : "+filename);

        Wini ini = new Wini();
        ini.getConfig().setLowerCaseOption(true); // make all options lower case to be more general
        ini.getConfig().setLowerCaseSection(true); // make all sections lower case to be more general
        try{
            ini.load(new File(filename));
            out = ini.get(section.toLowerCase(), key.toLowerCase());
            JRGSSLogger.println(DEBUG,"INI Read : "+out+" : "+filename);
        }catch(Exception e){
            JRGSSLogger.println(ERROR,"INI Read : "+filename);
        }
        if(out == null)
            return runtime.newFixnum(defaultVal);
        return runtime.newFixnum(Integer.parseInt(out));
    }
    //TODO - this only supports the value being a string. When getting support for the above read function done, it should be adapted to work here as well.
    public IRubyObject WritePrivateProfileString(ThreadContext context, IRubyObject[] args){

        String section = args[0].asJavaString();
        String key = args[1].asJavaString();
        String val = args[2].asJavaString();
        String filename = args[3].asJavaString();
        JRGSSLogger.println(PEDANTIC,"Internal WritePrivateProfileString() : "+filename);

        Wini ini = new Wini();
        ini.getConfig().setLowerCaseOption(true); // make all options lower case to be more general
        ini.getConfig().setLowerCaseSection(true); // make all sections lower case to be more general
        try{
            ini.load(new File(filename));
            ini.put(section.toLowerCase(), key.toLowerCase(), val);
            JRGSSLogger.println(DEBUG,"INI Write : "+val+" : "+filename);
        }catch(Exception e){
            JRGSSLogger.println(ERROR,"INI Write : "+filename);
        }
        return runtime.newFixnum(1);
    }

}
