package de.joo.AWEBlockBagTester;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class SchematicManager {
    private String baseDir;
    private static final String FORMAT_NAME = "sponge";

    public SchematicManager(File pluginDirectory) {
        File file = new File(pluginDirectory.getAbsoluteFile() + "/snapshots/");
        file.mkdirs();
        this.baseDir = file.getAbsolutePath();
    }

    public void saveSubRegionSchematic(String path, Clipboard clipboard) throws IOException {
        File file = new File(path);
        if(!file.isAbsolute()) {
            file = new File(this.baseDir, path);
        }
        saveClipboard(clipboard, file);
    }

    public boolean hasSchematic(String path) {
        File file = new File(path);
        if(!file.isAbsolute()) {
            file = new File(this.baseDir, path);
        }
        return file.exists();
    }

    public Clipboard loadSchematic(String path) throws IOException {
        File file = new File(path);
        if(!file.isAbsolute()) {
            file = new File(this.baseDir, path);
        }
        return getSchematic(file);
    }

    public void removeSchematic(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    private Clipboard getSchematic(File file) throws IOException {
        if(file == null || !file.exists()) {
            throw new IOException("No File found with Path: " + (file == null ? "null" : file.getAbsolutePath()) + ".");
        }
        ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(file);
        if(clipboardFormat == null) {
            throw new IOException("No clipboard found by file name " + file.getAbsolutePath());
        }

        try(ClipboardReader reader = clipboardFormat.getReader(new FileInputStream(file))) {
            return reader.read();

        }
    }

    private void saveClipboard(Clipboard clipboard, File file) throws IOException {
        ClipboardFormat format = ClipboardFormats.findByAlias(FORMAT_NAME);
        if (format == null) {
            throw new IOException("Unknown schematic format: " + FORMAT_NAME);
        }
        File parent = file.getParentFile();
        if(parent != null && !parent.exists()) {
            if(!parent.mkdirs())
                throw new IOException("Could not create folder for schematics.");
        }

        try(ClipboardWriter writer = format.getWriter(new FileOutputStream(file))) {
            writer.write(clipboard);
        }
    }
}
