package codechicken.lib.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

public abstract class ConfigTagParent {

    public static class TagOrderComparator implements Comparator<ConfigTag> {

        int sortMode;

        public TagOrderComparator(int sortMode) {
            this.sortMode = sortMode;
        }

        public int compare(ConfigTag o1, ConfigTag o2) {
            if (o1.position != o2.position) return Integer.compare(o1.position, o2.position);
            if (o1.brace != o2.brace) return o1.brace ? 1 : -1; // braced one goes after

            if (sortMode == 1) {
                if (Objects.equals(o1.value, o2.value)) return 0;
                if (o1.value == null) return 1;
                if (o2.value == null) return -1;
                return o1.value.compareTo(o2.value);
            } else {
                return o1.name.compareTo(o2.name);
            }
        }

    }

    /**
     * The immediate child tags of this tag
     */
    private final TreeMap<String, ConfigTag> childtags = new TreeMap<>();
    /**
     * A map of all tags in the tree, meaning all tags in this tag and all tags in child tags (recursively). Allows for
     * quick access to any tag in the tree, without the need to traverse the tree part by part.
     */
    private final Map<String, ConfigTag> allTagsInTree = new HashMap<>();
    public String comment;
    /**
     * 0 = name, 1 = value
     */
    public int sortMode = 0;
    /**
     * The mode for determining when child tags should leave a blank line between them and the one above 0 = never, 1 =
     * when braced, 2 = always
     */
    public int newlinemode = 1;

    public abstract void saveConfig();

    public abstract String getNameQualifier();

    public ConfigTagParent setComment(String comment) {
        this.comment = comment;
        saveConfig();
        return this;
    }

    public ConfigTagParent setSortMode(int mode) {
        sortMode = mode;
        saveConfig();
        return this;
    }

    public ConfigTagParent setNewLineMode(int mode) {
        newlinemode = mode;
        for (Entry<String, ConfigTag> entry : childtags.entrySet()) {
            ConfigTag tag = entry.getValue();
            if (newlinemode == 0) tag.newline = false;
            else if (newlinemode == 1) tag.newline = tag.brace;
            else if (newlinemode == 2) tag.newline = true;
        }
        saveConfig();
        return this;
    }

    public Map<String, ConfigTag> childTagMap() {
        return childtags;
    }

    public boolean hasChildTags() {
        return !childtags.isEmpty();
    }

    public boolean containsTag(String tagname) {
        return allTagsInTree.get(tagname) != null;
    }

    public ConfigTag getNewTag(String tagname) {
        return new ConfigTag(this, tagname);
    }

    public ConfigTag getTag(String tagname, boolean create) {
        ConfigTag tag = allTagsInTree.get(tagname);
        if (tag == null && create) return getTagRecursive(tagname, create);
        return tag;
    }

    public ConfigTag getTagRecursive(String tagname, boolean create) {
        int dotpos = tagname.indexOf(".");
        String basetagname = dotpos == -1 ? tagname : tagname.substring(0, dotpos);
        ConfigTag basetag = childtags.get(basetagname);
        if (basetag == null) {
            if (!create) return null;

            basetag = getNewTag(basetagname);
            saveConfig();
        }
        if (dotpos == -1) return basetag;

        return basetag.getTag(tagname.substring(dotpos + 1), create);
    }

    public ConfigTag getTag(String tagname) {
        return getTag(tagname, true);
    }

    public boolean removeTag(String tagname) {
        removeChildFromFlatMap(tagname);

        ConfigTag tag = getTag(tagname, false);
        if (tag == null) return false;

        int dotpos = tagname.lastIndexOf(".");
        String lastpart = dotpos == -1 ? tagname : tagname.substring(dotpos + 1, tagname.length());
        if (tag.parent != null) {
            boolean ret = tag.parent.childtags.remove(lastpart) != null;
            if (ret) saveConfig();
            return ret;
        }

        return false;
    }

    public void addChild(ConfigTag tag) {
        childtags.put(tag.name, tag);

        addChildToFlatMap(tag.name, tag);
    }

    private void addChildToFlatMap(String path, ConfigTag tag) {
        allTagsInTree.put(path, tag);

        // Stupid cast but these two garbage classes are interlinked anyway
        if ((this instanceof ConfigTag t) && t.parent != null) t.parent.addChildToFlatMap(t.name + "." + path, tag);
    }

    private void removeChildFromFlatMap(String path) {
        allTagsInTree.remove(path);

        // Stupid cast but these two garbage classes are interlinked anyway
        if ((this instanceof ConfigTag t) && t.parent != null) t.parent.removeChildFromFlatMap(t.name + "." + path);
    }

    public <T extends ConfigTag> ArrayList<T> getSortedTagList() {
        ArrayList<T> taglist = new ArrayList<T>(childtags.size());
        for (Entry<String, ConfigTag> tag : childtags.entrySet()) taglist.add((T) tag.getValue());

        Collections.sort(taglist, new TagOrderComparator(sortMode));
        return taglist;
    }

    public void loadChildren(BufferedReader reader) {
        String comment = "";
        String bracequalifier = "";
        try {
            while (true) {
                String line = ConfigFile.readLine(reader);
                if (line == null) break;
                if (line.startsWith("#")) {
                    if (comment.equals("")) comment = line.substring(1);
                    else comment = comment + "\n" + line.substring(1);
                } else if (line.contains("=")) {
                    String qualifiedname = line.substring(0, line.indexOf("="));
                    getTag(qualifiedname).onLoaded().setComment(comment)
                            .setValue(line.substring(line.indexOf("=") + 1));
                    comment = "";
                    bracequalifier = qualifiedname;
                } else if (line.equals("{")) {
                    getTag(bracequalifier).setComment(comment).useBraces().loadChildren(reader);
                    comment = "";
                    bracequalifier = "";
                } else if (line.equals("}")) {
                    break;
                } else {
                    bracequalifier = line;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveTagTree(PrintWriter writer, int tabs, String bracequalifier) {
        boolean first = true;
        for (ConfigTag tag : getSortedTagList()) {
            tag.save(writer, tabs, bracequalifier, first);
            first = false;
        }
    }

    public void writeComment(PrintWriter writer, int tabs) {
        if (comment != null && !comment.equals("")) {
            String[] comments = comment.split("\n");
            for (int i = 0; i < comments.length; i++) ConfigFile.writeLine(writer, "#" + comments[i], tabs);
        }
    }
}
