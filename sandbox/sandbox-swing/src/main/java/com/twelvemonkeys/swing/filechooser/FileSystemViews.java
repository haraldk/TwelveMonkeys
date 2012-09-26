package com.twelvemonkeys.swing.filechooser;

import com.twelvemonkeys.lang.Platform;
import com.twelvemonkeys.lang.Validate;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.FileChooserUI;
import java.io.File;
import java.io.IOException;

/**
 * FileSystemViews
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: FileSystemViews.java,v 1.0 Jan 14, 2010 3:19:51 PM haraldk Exp$
 */
public final class FileSystemViews {

    public static FileSystemView getFileSystemView() {
        if (Platform.os() == Platform.OperatingSystem.MacOS) {
            return ProxyFileSystemView.instance;
        }

        return FileSystemView.getFileSystemView();
    }

    private static class ProxyFileSystemView extends FileSystemView {

        private static final FileSystemView instance = createFSV();

        private static FileSystemView createFSV() {
            FileSystemView view = FileSystemView.getFileSystemView();

            try {
                FileChooserUI ui = null;
/* NOTE: The following is faster, but does not work reliably, as getSystemTypeDescription will return null...

                // The below is really a lot of hassle to avoid creating a JFileChooser. Maybe not a good idea?
                String uiClassName = UIManager.getString("FileChooserUI");
                try {
                    @SuppressWarnings({"unchecked"})
                    Class<FileChooserUI> uiClass = (Class<FileChooserUI>) Class.forName(uiClassName);
                    @SuppressWarnings({"unchecked"})
                    Constructor<FileChooserUI>[] constructors = uiClass.getDeclaredConstructors();
                    for (Constructor constructor : constructors) {
                        if (!constructor.isAccessible()) {
                            constructor.setAccessible(true);
                        }

                        Class[] parameterTypes = constructor.getParameterTypes();

                        // Test the two most likely constructors
                        if (parameterTypes.length == 0) {
                            ui = (FileChooserUI) constructor.newInstance();
                            break;
                        }
                        else if (parameterTypes.length == 1 && parameterTypes[0] == JFileChooser.class) {
                            ui = (FileChooserUI) constructor.newInstance((JFileChooser) null);
                            break;
                        }
                    }
                }
                catch (Exception ignore) {
                    ignore.printStackTrace();
                }

                if (ui == null) {
*/
                    // Somewhat slower, but should work even if constructors change
                    ui = new JFileChooser().getUI();
//                }

                return new ProxyFileSystemView(ui.getFileView(null), view);
            }
            catch (Throwable ignore) {
            }

            // Fall back to default view
            return view;
        }

        private final FileView uiView;
        private final FileSystemView defaultView;

        public ProxyFileSystemView(final FileView pUIView, final FileSystemView pDefaultView) {
            Validate.notNull(pUIView, "uiView");
            Validate.notNull(pDefaultView, "defaultFileSystemView");

            uiView = pUIView;
            defaultView = pDefaultView;
        }

        @Override
        public Boolean isTraversable(File f) {
            return uiView.isTraversable(f);
        }

        @Override
        public String getSystemDisplayName(File f) {
            return uiView.getName(f);
        }

        @Override
        public String getSystemTypeDescription(File f) {
            // TODO: Create something that gives a proper description here on the Mac...
            return uiView.getTypeDescription(f);
        }

        @Override
        public Icon getSystemIcon(File f) {
            return uiView.getIcon(f);
        }

        @Override
        public boolean isRoot(File f) {
            return defaultView.isRoot(f);
        }

        @Override
        public boolean isParent(File folder, File file) {
            return defaultView.isParent(folder, file);
        }

        @Override
        public File getChild(File parent, String fileName) {
            return defaultView.getChild(parent, fileName);
        }

        @Override
        public boolean isFileSystem(File f) {
            return defaultView.isFileSystem(f);
        }

        @Override
        public boolean isHiddenFile(File f) {
            return defaultView.isHiddenFile(f);
        }

        @Override
        public boolean isFileSystemRoot(File dir) {
            return defaultView.isFileSystemRoot(dir);
        }

        @Override
        public boolean isDrive(File dir) {
            return defaultView.isDrive(dir);
        }

        @Override
        public boolean isFloppyDrive(File dir) {
            return defaultView.isFloppyDrive(dir);
        }

        @Override
        public boolean isComputerNode(File dir) {
            return defaultView.isComputerNode(dir);
        }

        @Override
        public File[] getRoots() {
            return defaultView.getRoots();
        }

        @Override
        public File getHomeDirectory() {
            return defaultView.getHomeDirectory();
        }

        @Override
        public File getDefaultDirectory() {
            return defaultView.getDefaultDirectory();
        }

        @Override
        public File createFileObject(File dir, String filename) {
            return defaultView.createFileObject(dir, filename);
        }

        @Override
        public File createFileObject(String path) {
            return defaultView.createFileObject(path);
        }

        @Override
        public File[] getFiles(File dir, boolean useFileHiding) {
            return defaultView.getFiles(dir, useFileHiding);
        }

        @Override
        public File getParentDirectory(File dir) {
            return defaultView.getParentDirectory(dir);
        }

        @Override
        public File createNewFolder(File containingDir) throws IOException {
            return defaultView.createNewFolder(containingDir);
        }

        @Override
        public String toString() {
            return super.toString() + "[" + uiView + ", " + defaultView + "]";
        }
    }
}
