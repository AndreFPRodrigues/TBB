package tbb.core.logger;

import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import tbb.core.service.TBBService;

public class Zipping {
	private static final int BUFFER = 2048;
	private final static String SUBTAG = "Zipping: ";

    public static boolean Zip(ArrayList<String> filesAndDirectories, String zipFile){

        try {
            OutputStream out = new FileOutputStream(zipFile);
            Closeable res = out;

            try {
                ZipOutputStream zout = new ZipOutputStream(out);
                res = zout;
                for (String s : filesAndDirectories) {
                    File f = new File(s);

                    if (f.isDirectory()) {
                        Log.v(TBBService.TAG, SUBTAG + "zipping folder " + f.getName());
                        String name = f.getParent() + "/" + f.getName() + ".zip";
                        File resZip = new File(name);
                        try {
                            ZipDirectory(f, resZip);
                        }
                        catch (ZipException ze){
                            // if there were no entries in the folder, then ignore
                            if(ze.getMessage().equalsIgnoreCase("No entries")){
                                // delete folder zip
                                resZip.delete();
                                continue;
                            }
                            throw ze;
                        }

                        // add folder zip
                        String nameRef = f.getParentFile().toURI().relativize(resZip.toURI()).getPath();
                        ZipEntry entry = new ZipEntry(nameRef);
                        entry.setTime(resZip.lastModified());
                        zout.putNextEntry(entry);
                        copy(resZip, zout);
                        zout.closeEntry();

                        // delete folder zip
                        resZip.delete();
                    } else {
                        Log.v(TBBService.TAG, SUBTAG + "zipping file " + f.getName());

                        String nameRef = f.getParentFile().toURI().relativize(f.toURI()).getPath();
                        ZipEntry entry = new ZipEntry(nameRef);
                        entry.setTime(f.lastModified());
                        zout.putNextEntry(entry);
                        copy(f, zout);
                        zout.closeEntry();
                        
                        if(f.getName().contains(".zip"))
                        	f.delete();
                    }
                }
            }
            finally {
                res.close();
            }
        }
        catch (Exception e){
            if(e.getMessage().equalsIgnoreCase("No entries")){
                // delete folder zip
                (new File(zipFile)).delete();
                return true;
            }
            else {
                e.printStackTrace();
                Log.v(TBBService.TAG, SUBTAG + "Error while zipping: " + e.getMessage());
    			TBBService.writeToErrorLog(e);

            }
            return false;
        }

        return true;
    }

    private static void ZipDirectory(File directory, File zipfile) throws IOException {
        URI base = directory.getParentFile().toURI();
        Deque<File> queue = new LinkedList<File>();
        queue.push(directory);

        OutputStream out = new FileOutputStream(zipfile);
        Closeable res = out;
        try {
            ZipOutputStream zout = new ZipOutputStream(out);
            res = zout;
            while (!queue.isEmpty()) {
                directory = queue.pop();

                for (File kid : directory.listFiles()) {
                    String name = base.relativize(kid.toURI()).getPath();
                    if (kid.isDirectory()) {
                        queue.push(kid);
                        name = name.endsWith("/") ? name : name + "/";
                        ZipEntry entry = new ZipEntry(name);
                        entry.setTime(kid.lastModified());
                        zout.putNextEntry(entry);
                    } else {
                        ZipEntry entry = new ZipEntry(name);
                        entry.setTime(kid.lastModified());
                        zout.putNextEntry(entry);
                        copy(kid, zout);
                        zout.closeEntry();
                    }
                }
            }
        } finally {

            res.close();
        }
    }

    private static void UnzipDirectory(File zipfile, File directory) throws IOException {

        ZipFile zfile = new ZipFile(zipfile);
        Enumeration<? extends ZipEntry> entries = zfile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File file = new File(directory, entry.getName());
            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                file.getParentFile().mkdirs();
                InputStream in = zfile.getInputStream(entry);
                try {
                    copy(in, file);
                } finally {
                    in.close();
                }
            }
        }
    }

    /*
     *  UTIL METHODS
     */
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER];
        while (true) {
            int readCount = in.read(buffer);
            if (readCount < 0) {
                break;
            }
            out.write(buffer, 0, readCount);
        }
    }

    private static void copy(File file, OutputStream out) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            copy(in, out);
        } finally {
            in.close();
        }
    }

    private static void copy(InputStream in, File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        try {
            copy(in, out);
        } finally {
            out.close();
        }
    }
}