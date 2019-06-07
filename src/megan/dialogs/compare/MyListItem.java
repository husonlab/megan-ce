package megan.dialogs.compare;

import jloda.util.CanceledException;
import megan.core.Director;
import megan.core.Document;

import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;

/**
 * list item
 * Daniel Huson 2007
 */
public class MyListItem implements Comparator<MyListItem>, Serializable {
    private final int pid;
    private final String name;
    private final Document.ReadAssignmentMode readAssignmentMode;

    MyListItem(Director dir) {
        pid = dir.getID();
        name = dir.getTitle();
        readAssignmentMode = dir.getDocument().getReadAssignmentMode();
    }

    MyListItem(String fileName, boolean loadReadAssignmentMode) throws IOException, CanceledException {
        pid = -1;
        this.name = fileName;
        if (loadReadAssignmentMode) {
            final Document doc = new Document();
            doc.getMeganFile().setFileFromExistingFile(fileName, true);
            doc.loadMeganFile();
            readAssignmentMode = doc.getReadAssignmentMode();
        } else
            readAssignmentMode = null;
    }

    public String toString() {
        String str = "";
        if (getPID() > 0) {
            str += "[" + getPID() + "] ";
        }
        str += name;
        if (readAssignmentMode != null && readAssignmentMode != Document.ReadAssignmentMode.readCount)
            str += " [" + readAssignmentMode.toString() + "]";
        return str;
    }

    public int getPID() {
        return pid;
    }

    String getName() {
        return name;
    }

    public Document.ReadAssignmentMode getReadAssignmentMode() {
        return readAssignmentMode;
    }

    public int compare(MyListItem one, MyListItem other) {
        int x = one.getName().compareTo(other.getName());
        if (x != 0)
            return x;
        return Integer.compare(one.getPID(), other.getPID());
    }
}
