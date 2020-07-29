package SeleniumHelpers;


import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

public class K1Logger {

    public Logger setUpLogger(String fileName) {
        Logger logger = Logger.getLogger(fileName);
        Enumeration app = logger.getAllAppenders();
        if(app.hasMoreElements()){
            return logger; // keep using same logger already created. I think this solves the case where tests are Parameterized
        }
        FileAppender apndr=null;
        try {
            apndr = new FileAppender(new PatternLayout("%d{HH:mm:ss.SSS} %C{1}.%-25M: %m%n"),"target"+ File.separator+fileName+".log",false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.addAppender(apndr);
        logger.setLevel((Level) Level.ALL);
        return logger;
    }
}

