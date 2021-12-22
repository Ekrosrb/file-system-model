package com.ekros.cp.model;

import com.ekros.cp.util.Log;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class FileSystem implements Serializable {

  public static final transient int MAX_DESCRIPTORS = 5;
  public static final transient int MAX_BLOCKS = 10;
  public static final transient int MAX_LINKS_AMOUNT = 3;
  public static final transient int MAX_FILE_NAME_LENGTH = 10;

  private final Map<Integer, Descriptor> descriptors;
  private final List<Block> blocks;
  private Descriptor directory;
  private final Map<Integer, Descriptor> openFiles;

  public FileSystem() {
    directory = new Descriptor(true, false, null, 0, null, null);
    descriptors = new HashMap<>();
    blocks = new ArrayList<>();
    openFiles = new HashMap<>();
    init();
  }

  public boolean changeDirectory(String path) {
    Descriptor descriptor = getByPath(path, false);
    if (descriptor == null) {
      return false;
    }
    directory = descriptor;
    return true;
  }

  public boolean createSymlink(String str, String path) {
    if (path == null || str == null) {
      return false;
    }
    String name = getFileName(path);
    Descriptor parent = getParent(path);

    if (parent == null || isDescriptorsMax() || !checkName(name, parent)) {
      return false;
    }

    Descriptor descriptor = new Descriptor(false, true, null,
        Block.MAX_BLOCK_SIZE, parent, str);
    int index = getFreeIndex(descriptors);
    parent.addDescriptor(name, descriptor);
    descriptors.put(index, descriptor);
    return true;
  }

  public boolean createDirectory(String path) {
    Descriptor descriptor = getByPath(path, true);
    return descriptor != null;
  }

  private Descriptor getByPath(String path, boolean withCreate) {
    Descriptor descriptor = directory;
    if (path.startsWith("~/")) {
      descriptor = getRoot();
      path = path.substring(2);
    }
    return pathDescriptor(path, descriptor, withCreate, false);
  }

  private Descriptor getRoot() {
    Descriptor descriptor = directory;
    while (descriptor.getPrev() != null) {
      descriptor = descriptor.getPrev();
    }
    return descriptor;
  }

  private Descriptor pathDescriptor(String path, Descriptor descriptor, boolean withCreate, boolean isFile) {
    if(path.startsWith("/")){
      return null;
    }
    if (path.isEmpty()) {
      return descriptor;
    }

    String[] pathArray = path.split("/");

    String next = pathArray[0];
    int indexOf = path.indexOf('/');
    String nextPath = "";
    if (pathArray.length != 1 && indexOf != -1) {
      nextPath = path.substring(indexOf).substring(1);
    }
    Descriptor nextDescriptor;
    if (next.equals("..")) {
      nextDescriptor = pathDescriptor(nextPath, descriptor.getPrev(), withCreate, isFile);
    } else if (next.equals(".")) {
      nextDescriptor = pathDescriptor(nextPath, descriptor, withCreate, isFile);
    } else if (next.equals("~")) {
      nextDescriptor = pathDescriptor(nextPath, getRoot(), withCreate, isFile);
    } else if (pathArray.length == 1) {
      nextDescriptor = descriptor.getByName(next);
      if (nextDescriptor == null && !isFile) {
        if (!withCreate) {
          return null;
        }
        boolean status = createDirectory(next, descriptor);
        if (!status) {
          return null;
        }
      }
      if (nextDescriptor != null && nextDescriptor.isSymlink()) {
        nextPath = nextDescriptor.getSymlink() + nextPath;
        nextDescriptor = pathDescriptor(nextPath, descriptor, withCreate, isFile);
      } else {
        nextDescriptor = descriptor.getByName(next);
      }
    } else {
      nextDescriptor = descriptor.getByName(next);
      if (nextDescriptor == null) {
        if (!withCreate) {
          return null;
        }
        boolean status = createDirectory(next, descriptor);
        if (!status) {
          return null;
        }
      }
      if (nextDescriptor != null && nextDescriptor.isSymlink()) {
        nextPath = nextDescriptor.getSymlink() + nextPath;
        nextDescriptor = pathDescriptor(nextPath, descriptor, withCreate, isFile);
      } else {
        nextDescriptor = pathDescriptor(nextPath, descriptor.getByName(next), withCreate, isFile);
      }
    }

    return nextDescriptor;
  }

  public boolean removeDirectory(String path) {
    String name = getFileName(path);
    Descriptor parent = getParent(path);

    if (parent == null || name == null) {
      return false;
    }

    Descriptor dir = parent.getByName(name);

    if (dir == null || !dir.isDirectory() || dir.getNameLinks().size() > 1) {
      return false;
    }
    parent.removeDescriptor(name);
    removeDescriptor(dir);
    return true;
  }

  public boolean addFile(String path) {
    if (path == null) {
      return false;
    }
    String name = getFileName(path);
    Descriptor parent = getParent(path);

    if (parent == null || isDescriptorsMax() || !checkName(name, parent)) {
      return false;
    }

    int blockId = getFreeBlockId();

    if (blockId == -1) {
      return false;
    }
    blocks.get(blockId).setUsed(true);
    Descriptor descriptor = new Descriptor(false, false, blockId, Block.MAX_BLOCK_SIZE, parent,
        null);
    int index = getFreeIndex(descriptors);
    parent.addDescriptor(name, descriptor);
    descriptors.put(index, descriptor);
    return true;
  }

  public boolean write(int fd, int offset, int size) {
    if (!openFiles.containsKey(fd)) {
      Log.error("Incorrect fd [" + fd + "]");
      return false;
    }
    String data = getFileData(fd);
    if (size > data.length() || offset > data.length() || size + offset > data.length()
        || offset < 0 || size <= 0) {
      Log.error("Incorrect offset or size");
      return false;
    }
    data = data.substring(0, offset) + "1".repeat(size) + data.substring(offset + size);
    int dataOffset = 0;
    for (Block block : getFileBlocks(openFiles.get(fd))) {
      block.setData(data.substring(dataOffset, dataOffset + Block.MAX_BLOCK_SIZE));
      dataOffset += Block.MAX_BLOCK_SIZE;
    }
    return true;
  }

  public String read(int fd, int offset, int size) {
    if (!openFiles.containsKey(fd)) {
      Log.error("Incorrect fd");
      return "";
    }
    String data = getFileData(fd);
    if (size > data.length() || offset > data.length() || size <= 0 || offset < 0) {
      Log.error("Incorrect offset or size");
      return "";
    }
    return data.substring(offset, offset + size);
  }

  public boolean truncate(String path, int size) {

    String name = getFileName(path);
    Descriptor parent = getParent(path);

    if (parent == null || name == null) {
      return false;
    }

    Descriptor descriptor = parent.getByName(name);
    if (descriptor == null || size < 0 || descriptor.isDirectory() || descriptor.isSymlink()) {
      return false;
    }

    int extension = size - descriptor.getSize();
    if (extension > 0) {
      List<Integer> blocksIds = getFreeBlocksIdsForExtension(extension);
      if (blocksIds.isEmpty()) {
        return false;
      }
      descriptor.getBlockLinks().addAll(blocksIds);
    } else if (extension < 0) {
      while (extension <= -Block.MAX_BLOCK_SIZE) {
        Integer id = descriptor.getBlockLinks().remove(descriptor.getBlockLinks().size() - 1);
        blocks.get(id).setUsed(false);
        extension += Block.MAX_BLOCK_SIZE;
      }
    }
    descriptor.setSize(size);
    return true;
  }

  public boolean openFile(String path) {

    String name = getFileName(path);
    Descriptor parent = getParent(path);

    if (parent == null || name == null) {
      return false;
    }

    Descriptor descriptor = parent.getByName(name);
    if (descriptor == null || descriptor.isDirectory() || descriptor.isSymlink()) {
      return false;
    }
    int index = getFreeIndex(openFiles);
    openFiles.put(index, descriptor);
    Log.info("File [" + name + "] open with fd " + index);
    return true;
  }

  public boolean link(String path, String name2) {

    String name = getFileName(path);
    Descriptor parent = getParent(path);

    if (parent == null || name == null) {
      return false;
    }

    Descriptor descriptor = parent.getByName(name);
    if (descriptor == null || !checkName(name2, parent) || descriptor.isDirectory()
        || descriptor.isSymlink()) {
      return false;
    }
    parent.addDescriptor(name2, descriptor);
    return true;
  }

  public boolean unlink(String path) {

    Descriptor parent = getParent(path);

    String name = getFileName(path);


    if (parent == null || name == null) {
      return false;
    }

    Descriptor descriptor = parent.removeDescriptor(name);
    if (descriptor == null || descriptor.isDirectory() || descriptor.isSymlink()) {
      return false;
    }

    if (!parent.contains(descriptor)) {
      removeDescriptor(descriptor);
    }

    if (!openFiles.containsValue(descriptor)) {
      getFileBlocks(descriptor).forEach(block -> block.setUsed(false));
    }
    return true;
  }

  public boolean closeFile(int fd) {
    Descriptor descriptor = openFiles.remove(fd);

    if (descriptor != null && !openFiles.containsValue(descriptor) &&
        !descriptors.containsValue(descriptor)) {
      getFileBlocks(descriptor).forEach(block -> block.setUsed(false));
    }

    return descriptor != null;
  }

  public String getLinksInfo() {

    StringBuilder sb = new StringBuilder("\n");

    Set<Entry<String, Descriptor>> entries = directory.getNameLinks().entrySet();
    for (Entry<String, Descriptor> entry : entries) {
      int id = getDescriptorIndex(entry.getValue());
      sb.append(entry.getKey()).append(" : ").append(id).append("  ")
          .append(entry.getValue().isDirectory() ? "Directory"
              : (entry.getValue().isSymlink() ? "Symlink" : "File")).append("\n");
    }

    return sb.toString();
  }

  private String getFileName(String path){
    if(path == null){
      return null;
    }
    int index = path.lastIndexOf('/');
    if(index != -1 && path.split("/").length != 1){
      return path.substring(index+1);
    }
    return path;
  }

  public boolean format() {
    descriptors.clear();
    directory = new Descriptor(true, false, null, 0, null, null);
    return true;
  }

  private Descriptor getParent(String path) {
    String[] pathArray = path.split("/");
    Descriptor parent = directory;
    if (pathArray.length > 1) {
      parent = getByPath(path.substring(0, path.lastIndexOf('/')), false);
    }
    return parent;
  }

  private boolean createDirectory(String name, Descriptor parent) {
    if (!checkName(name, parent) || isDescriptorsMax()) {
      return false;
    }
    Descriptor descriptor = new Descriptor(true, false, null, 0, parent, null);
    int index = getFreeIndex(descriptors);
    descriptors.put(index, descriptor);
    parent.addDescriptor(name, descriptor);
    return true;
  }

  private void init() {
    for (int i = 0; i < MAX_BLOCKS; i++) {
      blocks.add(i, new Block());
    }
  }

  private void removeDescriptor(Descriptor descriptor) {
    descriptors.entrySet().removeIf(entry -> descriptor.equals(entry.getValue()));
  }

  private List<Block> getFileBlocks(Descriptor descriptor) {
    return descriptor.getBlockLinks().stream().map(blocks::get).collect(Collectors.toList());
  }

  private List<Integer> getFreeBlocksIdsForExtension(int extension) {
    int count = (int) Math.ceil((double) extension / Block.MAX_BLOCK_SIZE);
    if (count > blocks.size()) {
      return Collections.emptyList();
    }
    List<Integer> blocksIds = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Integer id = getFreeBlockId();
      if (id == -1) {
        blocksIds.forEach(bid -> blocks.get(bid).setUsed(false));
        return Collections.emptyList();
      }
      blocksIds.add(id);
      blocks.get(id).setUsed(true);
    }
    return blocksIds;
  }

  private int getFreeIndex(Map<Integer, Descriptor> map) {
    for (int i = 0; i < map.size(); i++) {
      if (map.get(i) == null) {
        return i;
      }
    }
    return map.size();
  }

  private int getDescriptorIndex(Descriptor descriptor) {
    for (Entry<Integer, Descriptor> entry : descriptors.entrySet()) {
      if (entry.getValue().equals(descriptor)) {
        return entry.getKey();
      }
    }
    return -1;
  }

  private Integer getFreeBlockId() {
    return blocks.indexOf(blocks.stream()
        .filter(block -> !block.isUsed())
        .findFirst().orElse(null));
  }

  private boolean checkName(String name, Descriptor parent) {
    return name != null && name.length() <= MAX_FILE_NAME_LENGTH && !parent.contains(name);
  }

  private boolean isDescriptorsMax() {
    return MAX_DESCRIPTORS == descriptors.size();
  }

  private String getFileData(int fd) {
    Descriptor descriptor = openFiles.get(fd);
    List<String> dataSet = descriptor.getBlockLinks().stream()
        .map(link -> blocks.get(link).getData()).collect(Collectors.toList());
    StringBuilder sb = new StringBuilder();
    dataSet.forEach(sb::append);
    return sb.toString();
  }

}
