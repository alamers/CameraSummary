package nl.aardbeitje.camerasummary;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileGrouper {

    /* timegap between two photos to consider it separate events, in ms */
    private static final long TIMEGAP = 5*60*1000;
    private static final boolean SKIP_OPEN_EVENTS = true;

    public List<List<File>> groupFolder(File folder) {
        List<List<File>> groups = new ArrayList<>();
        List<File> group = new ArrayList<>();

        List<File> files = Arrays.asList(folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jpg");
            }
        }));

        if (files.isEmpty()) {
            return groups;
        }

        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return (int) (o1.lastModified() - o2.lastModified());
            }
        });

        File p = files.get(0);
        group.add(p);
        for (int i = 1; i < files.size(); i++) {
            File q = files.get(i);
            
            if (timeGapTooLarge(p, q)) {
                if (fileOldEnough(p)) {
                    groups.add(group);
                } // else drop entire group for now
                group = new ArrayList<>();
            }
            group.add(q);
            p = q;
        }

        if (fileOldEnough(p)) {
            groups.add(group);
        } // else drop entire group for now

        return groups;

    }

    private boolean fileOldEnough(File p) {
        return  !SKIP_OPEN_EVENTS || p.lastModified() + TIMEGAP < System.currentTimeMillis();
    }

    private boolean timeGapTooLarge(File p, File q) {
        return q.lastModified() - p.lastModified() > TIMEGAP;
    }

}
