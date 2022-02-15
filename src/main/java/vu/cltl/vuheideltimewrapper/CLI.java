package vu.cltl.vuheideltimewrapper;

import eu.kyotoproject.kaf.KafSaxParser;
import eu.kyotoproject.kaf.LP;
import org.apache.commons.cli.*;
import org.apache.tools.bzip2.CBZip2InputStream;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class CLI {

    static final String layer = "timex";
    static final String name = "vua-heideltime-wrapper";
    static final String version = "1.0";
    private static Logger logger = Logger.getLogger(CLI.class.getName());
    private static String CONFIG = "conf/wrapper.props";

    static private Options getOptions() {
        Options options = new Options();
        options.addOption(new Option("i", "input", true, "input naf file/folder"));
        options.addOption(new Option("o", "output", true, "output naf file/folder"));
        options.addOption(new Option("s", "stream", false, "input stream"));
        options.addOption(new Option("c", "config", true, "wrapper config"));
        options.addOption(new Option("r", "replace", false, "replace existing timex layer"));
        return options;
    }

    private static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "VU Heideltime wrapper", options);
        System.exit(0);
    }

    private static boolean validOptions(CommandLine cmd) {
        if (! (cmd.hasOption('i') || cmd.hasOption('o') || cmd.hasOption('s'))) {
            System.out.println("Missing input file/folder/stream");
            return false;
        }
        return true;
    }

    static public void main (String [] args) {
        Options options = getOptions();
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (! validOptions(cmd))
                usage(options);
            String configFile = cmd.getOptionValue('c', CONFIG);
            Properties props = loadConfig(configFile);
            if (cmd.hasOption('s'))
                processNafStream(System.in, props, cmd.hasOption('r'));
            else
                loop(cmd.getOptionValue('i'), cmd.getOptionValue('o'), cmd.hasOption('r'), props);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    protected static Properties loadConfig(String configFile) {
        Properties props = new Properties();
        try {
            FileReader reader = new FileReader(configFile);
            props.load(reader);
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }

    static public void processNafStream(InputStream nafStream, Properties props, boolean replace) {
        KafSaxParser kafSaxParser = new KafSaxParser();
        kafSaxParser.parseFile(nafStream);
        runHeildelTime(kafSaxParser, replace, props);
        kafSaxParser.writeNafToStream(System.out);
    }

    static public void processNafFile(String inputFile, String outFile, boolean replace, Properties props) {
        KafSaxParser kafSaxParser = new KafSaxParser();
        if (inputFile.toLowerCase().endsWith(".gz")) {
            try {
                InputStream fileStream = new FileInputStream(inputFile);
                InputStream gzipStream = new GZIPInputStream(fileStream);
                kafSaxParser.parseFile(gzipStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (inputFile.toLowerCase().endsWith(".bz2")) {
            try {
                InputStream fileStream = new FileInputStream(inputFile);
                InputStream gzipStream = new CBZip2InputStream(fileStream);
                kafSaxParser.parseFile(gzipStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            kafSaxParser.parseFile(inputFile);
        }
        runHeildelTime(kafSaxParser, replace, props);
        try {
            FileOutputStream fos = new FileOutputStream(outFile);
            kafSaxParser.writeNafToStream(fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loop(String indir, String outdir, boolean replace, Properties props)  {
        createPath(outdir);
        try (Stream<Path> paths = Files.walk(Paths.get(indir))) {
            paths.filter(p -> Files.isRegularFile(p)).filter(p -> {
                try {
                    return ! Files.isHidden(p);
                } catch (IOException e) {
                    return false;
                }}).forEach(f -> processNafFile(f.toString(), outputFileName(outdir, f), replace, props));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String outputFileName(String outDir, Path f) {
        return Paths.get(outDir, f.getFileName().toString()).toString();
    }

    public static void createPath(String outdir) {
        Path dirpath = Paths.get(outdir);
        if (!Files.exists(dirpath)) {
            try {
                Files.createDirectories(Paths.get(outdir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static void runHeildelTime(KafSaxParser kafSaxParser, boolean replace, Properties props) {

        String strTimeStamp = eu.kyotoproject.util.DateUtil.createTimestamp();
        boolean showBeginEndStamps = Boolean.parseBoolean(props.getProperty("show_begin_and_end_stamps"));
        String strBeginDate = showBeginEndStamps ? strTimeStamp: null;
        if (replace)
           kafSaxParser.kafTimexLayer = new ArrayList<>();

        try {
            String lang = kafSaxParser.getLanguage();
            logger.log(Level.INFO,"\tlang = " + lang
                    + "\n\tmappingFile = " + props.getProperty("mapping")
                    + "\n\tconfigFile = " + props.getProperty("heideltime_conf"));

            VuNafHeideltime time = new VuNafHeideltime(lang, props.getProperty("mapping"), props.getProperty("heideltime_conf"));

            time.process(kafSaxParser);
        }
        catch (Exception e){
              System.err.println("VuNafHeidelTime failed: ");
              e.printStackTrace();
        }

        String strEndDate = showBeginEndStamps ? eu.kyotoproject.util.DateUtil.createTimestamp() : null;
        String host = Boolean.parseBoolean(props.getProperty("show_host")) ? getHost(): null;
        LP lp = new LP(name, version, strTimeStamp, strBeginDate, strEndDate, host);
        kafSaxParser.getKafMetaData().addLayer(layer, lp);
    }

    private static String getHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }
}
