package nl.aardbeitje.camerasummary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimeZone;

public class CameraSummary {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm");
    private static String signalCliDir;

    public static void main(String[] args) throws IOException {
        
        if(args.length!=2) {
            System.out.println("Usage: java -jar camerasummary-0.0.1-SNAPSHOT.jar <images directory> <signal-cli-path>");
            System.exit(-1);
        }

        String imagesDir = args[0];
        signalCliDir = args[1];
        File dir = new File(imagesDir);
        recurse(dir);
    }

    private static void recurse(File dir) throws IOException {

        summarize(dir);

        File[] subdirs = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });
        for (File subdir : subdirs) {
            recurse(subdir);
        }
    }

    private static void summarize(File dir) throws IOException {

        List<List<File>> groups = new FileGrouper().groupFolder(dir);

        for (List<File> group : groups) {
            if (!group.isEmpty()) {
                File firstFile = group.get(0);

                String filename = createFileName(firstFile);
                File output = new File(firstFile.getParentFile().getAbsolutePath(), filename);

                System.out.println(output.getAbsolutePath());
                List<FileInputStream> is = toInputStreamList(group);

                OutputStream os = new FileOutputStream(output);
                GifSequenceWriter.compose(is, os);

                os.close();
                for (FileInputStream fis : is) {
                    fis.close();
                }

                for (File f : group) {
                    f.delete();
                }

                output.setLastModified(firstFile.lastModified());
                notify(output);

            }
        }

    }

    private static void notify(File output) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(signalCliDir, "-u", "+31623822405", "send", "-m", output.getName(), "+31623822405", "-a", output.getAbsolutePath().toString());

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static String createFileName(File firstFile) {
        ZonedDateTime localTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(firstFile.lastModified()), TimeZone.getDefault().toZoneId());
        String filename = "" + DATE_FORMAT.format(localTime) + ".gif";
        return filename;
    }

    private static List<FileInputStream> toInputStreamList(List<File> group) {
        return group.stream().map(f -> {
            try {
                return new FileInputStream(f);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

}
