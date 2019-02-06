package vu.cltl.vuheideltimewrapper;

import eu.kyotoproject.kaf.KafSaxParser;
import eu.kyotoproject.kaf.KafTimex;
import eu.kyotoproject.kaf.LP;
import org.apache.tools.bzip2.CBZip2InputStream;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public class CLI {

    static final String layer = "timex";
    static final String name = "vua-heideltime-wrapper";
    static final String version = "1.0";

    static final String usage = "\nCalls Heideltime to add timex layer to NAF"
                             +  "\n--naf-file       path to a naf input file"
                             +  "\n--naf-folder     path to a folder with naf input files, also specify the extension"
                             +  "\n--extension-in   extension of the input naf files in the folder"
                             +  "\n--extension-out  extension of the output naf files in the folder"
                             +  "\n--replace        replace existing timex layer"
                             +  "\n--stream         NAF text stream as input"
                             +  "\n--mapping        path to the mapping file"
                             +  "\n--config         path to the config file"
                             +  "\n--language       language"
                            ;
    static boolean STREAM = false;
    static boolean REPLACE = false;
    static String extensionIn = "";
    static String extensionOut = ".tmx.naf";
    static String mappingFile = "";
    static String configFile = "";
    static String language = "";

    static String folder = "";
    static String pathToNafFile = "";
    private static Logger logger = Logger.getLogger(CLI.class.getName());

    static public void processArgs (String [] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--naf-file") && args.length > (i + 1)) {
                pathToNafFile = args[i + 1];
            } else if (arg.equals("--naf-folder") && args.length > (i + 1)) {
                folder = args[i + 1];
            } else if (arg.equals("--extension-in") && args.length > (i + 1)) {
                extensionIn = args[i + 1];
            } else if (arg.equals("--extension-out") && args.length > (i + 1)) {
                extensionOut = args[i + 1];
            }else if (arg.equals("--mapping") && args.length > (i + 1)) {
                mappingFile = args[i + 1];
            }else if (arg.equals("--config") && args.length > (i + 1)) {
                configFile = args[i + 1];
            }else if (arg.equals("--language") && args.length > (i + 1)) {
                language = args[i + 1];
            } else if (arg.equalsIgnoreCase("--replace")) {
                REPLACE = true;
            } else if (arg.equalsIgnoreCase("--stream")) {
                STREAM = true;
            }
        }
    }
    static String testargs = "--naf-file /Code/vu/newsreader/vuheideltimewrapper/example/wikinews_1173_nl.input.naf --mapping /Code/vu/newsreader/vuheideltimewrapper/lib/alpino-to-treetagger.csv --config /Code/vu/newsreader/vuheideltimewrapper/conf/config.props";

    static public void main (String [] args) {
        if (args.length == 0) {
            args = testargs.split(" ");
        }
        processArgs(args);
        if (STREAM) {
            /// input and output stream
              processNafStream(System.in);
        }
        else {
            if (!folder.isEmpty()) {
                processNafFolder(new File(folder), extensionIn);
            } else {
                processNafFile(pathToNafFile);
            }
        }
    }



    static public void processNafStream (InputStream nafStream) {
        KafSaxParser kafSaxParser = new KafSaxParser();
        kafSaxParser.parseFile(nafStream);
        runHeildelTime(kafSaxParser);
        kafSaxParser.writeNafToStream(System.out);
    }

    static public void processNafFile (String pathToNafFile) {
        KafSaxParser kafSaxParser = new KafSaxParser();
        if (pathToNafFile.toLowerCase().endsWith(".gz")) {
            try {
                InputStream fileStream = new FileInputStream(pathToNafFile);
                InputStream gzipStream = new GZIPInputStream(fileStream);
                kafSaxParser.parseFile(gzipStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (pathToNafFile.toLowerCase().endsWith(".bz2")) {
            try {
                InputStream fileStream = new FileInputStream(pathToNafFile);
                InputStream gzipStream = new CBZip2InputStream(fileStream);
                kafSaxParser.parseFile(gzipStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            kafSaxParser.parseFile(pathToNafFile);
        }
        runHeildelTime(kafSaxParser);
        try {
            String filePath = pathToNafFile.substring(0,pathToNafFile.lastIndexOf("."));
            FileOutputStream fos = new FileOutputStream(filePath+extensionOut);
            kafSaxParser.writeNafToStream(fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public void processNafFolder (File pathToNafFolder, String extension) {
        ArrayList<File> files = makeRecursiveFileList(pathToNafFolder, extension);
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            processNafFile(file.getAbsolutePath());
        }
    }

    static public ArrayList<File> makeRecursiveFileList(File inputFile, String theFilter) {
            ArrayList<File> acceptedFileList = new ArrayList<File>();
            File[] theFileList = null;
            if ((inputFile.canRead())) {
                theFileList = inputFile.listFiles();
                for (int i = 0; i < theFileList.length; i++) {
                    File newFile = theFileList[i];
                    if (newFile.isDirectory()) {
                        ArrayList<File> nextFileList = makeRecursiveFileList(newFile, theFilter);
                        acceptedFileList.addAll(nextFileList);
                    } else {
                        if (newFile.getName().endsWith(theFilter)) {
                            acceptedFileList.add(newFile);
                        }
                    }
                }
            } else {
                System.out.println("Cannot access file:" + inputFile + "#");
                if (!inputFile.exists()) {
                    System.out.println("File/folder does not exist!");
                }
            }
            return acceptedFileList;
    }

    static void runHeildelTime (KafSaxParser kafSaxParser) {

        String strBeginDate = eu.kyotoproject.util.DateUtil.createTimestamp();
        String strEndDate = null;
        if (REPLACE) {
           kafSaxParser.kafTimexLayer = new ArrayList<KafTimex>();
        }
        try {
            String lang = kafSaxParser.getLanguage();
//            System.out.println("lang = " + lang);
//            System.out.println("mappingFile = " + mappingFile);
//            System.out.println("configFile = " + configFile);
            logger.log(Level.INFO,"\tlang = " + lang
                    + "\n\tmappingFile = " + mappingFile
                    + "\n\tconfigFile = " + configFile);

            VuNafHeideltime time = new VuNafHeideltime(lang, mappingFile, configFile);

            time.process(kafSaxParser);

        }
        catch (Exception e){
              System.err.println("VuNafHeidelTime failed: ");
              e.printStackTrace();
        }
        strEndDate = eu.kyotoproject.util.DateUtil.createTimestamp();
        String host = "";
        try {
           host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
           e.printStackTrace();
        }
        LP lp = new LP(name,version, strBeginDate, strBeginDate, strEndDate, host);
        kafSaxParser.getKafMetaData().addLayer(layer, lp);
    }



}
