package org.jrgss;

import lombok.Data;
import org.ini4j.Wini;
import org.ini4j.Config;

import java.io.File;

/**
 * @author matt
 * @date 8/25/14
 */
@Data
public class ConfigReader {

    Wini ini;

    public ConfigReader(String path) {
        try {
            this.ini = new Wini();
            this.ini.getConfig().setLowerCaseOption(true); // make all options lower case to be more general
            this.ini.getConfig().setLowerCaseSection(true); // make all sections lower case to be more general
            this.ini.load(new File(path));
        } catch (Exception io) {
            throw new RuntimeException("Could not read from ini file!");
        }
    }

    public String getTitle() {
        return ini.get("game", "title");
    }

    public String getScripts() {
        return ini.get("game", "scripts");
    }

    public RGSSVersion getRGSSVersion() {
        String library = ini.get("game", "library");
        if (library == null) {
            System.out.println("Could not determine RGSS Version. Using default!");
            return RGSSVersion.defaultVersion();
        }
        int dllIndex = library.toLowerCase().indexOf("rgss");
        if (dllIndex == -1) {
            System.out.println("Could not determine RGSS Version. Using default!");
            return RGSSVersion.defaultVersion();
        }
        RGSSVersion version = RGSSVersion.parse(library.substring(dllIndex));
        if(version == null) {
            System.out.println("Could not determine RGSS Version. Using default!");
            return RGSSVersion.defaultVersion();
        }
        return version;
    }

    public RTPVersion getRTPVersion() {
        String rtp = ini.get("game", "rtp");
        if(rtp == null) {
            return RTPVersion.None;
        }
        try{
            RTPVersion version = RTPVersion.valueOf(rtp);
            return version;
        } catch (IllegalArgumentException ex) {
            return RTPVersion.None;
        }
    }

}
