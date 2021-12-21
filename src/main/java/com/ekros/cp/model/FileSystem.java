package com.ekros.cp.model;

import com.ekros.cp.util.Log;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
  private final Descriptor directory;
  private final Map<Integer, Descriptor> openFiles;

  public FileSystem() {
    directory = new Descriptor(true, null, 0);
    descriptors = new HashMap<>();
    blocks = new ArrayList<>();
    openFiles = new HashMap<>();
    init();
  }

//  public boolean changeDirectory(String path){
//
//  }

  private Descriptor pathDescriptor(String path, Descriptor descriptor){
    return null;
  }

  public boolean createDirectory(String name){
    return true;
  }

  public boolean removeDirectory(String name){
    return true;
  }

  private void init() {
    for (int i = 0; i < MAX_BLOCKS; i++) {
      blocks.add(i, new Block());
    }
  }

  public boolean addFile(String name) {
    if (isDescriptorsMax() || !checkName(name)) {
      return false;
    }

    int blockId = getFreeBlockId();

    if (blockId == -1) {
      return false;
    }
    blocks.get(blockId).setUsed(true);
    Descriptor descriptor = new Descriptor(false, blockId, Block.MAX_BLOCK_SIZE);
    int index = getFreeIndex(descriptors);
    directory.addDescriptor(name, descriptor);
    descriptors.put(index, descriptor);
    return true;
  }

  public boolean write(int fd, int offset, int size){
    if(!openFiles.containsKey(fd)){
      Log.error("Incorrect fd [" + fd + "]");
      return false;
    }
    String data = getFileData(fd);
    if(size > data.length() || offset > data.length() || size+offset > data.length()
        || offset < 0 || size <= 0){
      Log.error("Incorrect offset or size");
      return false;
    }
    data = data.substring(0, offset) + "1".repeat(size) + data.substring(offset+size);
    int dataOffset = 0;
    for(Block block: getFileBlocks(openFiles.get(fd))) {
        block.setData(data.substring(dataOffset, dataOffset + Block.MAX_BLOCK_SIZE));
        dataOffset += Block.MAX_BLOCK_SIZE;
    }
    return true;
  }

  public String read(int fd, int offset, int size) {
    if(!openFiles.containsKey(fd)){
      Log.error("Incorrect fd");
      return "";
    }
    String data = getFileData(fd);
    if(size > data.length() || offset > data.length() || size <= 0 || offset < 0){
      Log.error("Incorrect offset or size");
      return "";
    }
    return data.substring(offset, offset+size);
  }

  public boolean truncate(String name, int size) {
    Descriptor descriptor = directory.getByName(name);
    if (descriptor == null || size < 0) {
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

  public boolean openFile(String name) {
    Descriptor descriptor = directory.getByName(name);
    if (descriptor == null || descriptor.isDirectory()) {
      return false;
    }
    int index = getFreeIndex(openFiles);
    openFiles.put(index, descriptor);
    Log.info("File [" + name + "] open with fd " + index);
    return true;
  }

  public boolean link(String name1, String name2) {
    Descriptor descriptor = directory.getByName(name1);
    if (descriptor == null || !checkName(name2)) {
      return false;
    }
    directory.addDescriptor(name2, descriptor);
    return true;
  }

  public boolean unlink(String name) {
    Descriptor descriptor = directory.removeDescriptor(name);
    if (descriptor == null) {
      return false;
    }

    if(!directory.contains(descriptor)){
      removeDescriptor(descriptor);
    }

    if(!openFiles.containsValue(descriptor)){
      getFileBlocks(descriptor).forEach(block -> block.setUsed(false));
    }
    return true;
  }

  public boolean closeFile(int fd) {
    Descriptor descriptor = openFiles.remove(fd);

    if(descriptor != null && !openFiles.containsValue(descriptor) &&
        !descriptors.containsValue(descriptor)){
      getFileBlocks(descriptor).forEach(block -> block.setUsed(false));
    }

    return descriptor != null;
  }

  public String getLinksInfo() {

    StringBuilder sb = new StringBuilder();

    Set<Entry<String, Descriptor>> entries = directory.getNameLinks().entrySet();
    for (Entry<String, Descriptor> entry : entries) {
      int id = getDescriptorIndex(entry.getValue());
      sb.append(entry.getKey()).append(" : ").append(id).append("\n");
    }

    return sb.toString();
  }

//  public boolean format(int n) {
//    if (n > descriptors.size()) {
//      Log.error("Incorrect size [" + n + "] max value: " + descriptors.size());
//      return false;
//    }
//
//    Iterator<Integer> keys = descriptors.keySet().iterator();
//
//    Log.info("Descriptors before format: " + descriptors.keySet());
//
//    for(int i = 0; i < n && keys.hasNext(); i++){
//      Integer key = keys.next();
//      Descriptor descriptor = descriptors.remove(key);
//      Set<String> dirKeys = directory.getDescriptorLinks(descriptor);
//      dirKeys.forEach(directory::removeDescriptor);
//      List<Block> blocks = getFileBlocks(descriptor);
//      blocks.forEach(block -> block.setUsed(false));
//
//    }
//
//    Log.info("Descriptors after format: " + descriptors.keySet());
//    return true;
//  }

  private void removeDescriptor(Descriptor descriptor){
    descriptors.entrySet().removeIf(entry-> descriptor.equals(entry.getValue()));
  }

  private List<Block> getFileBlocks(Descriptor descriptor) {
    return descriptor.getBlockLinks().stream().map(blocks::get).collect(Collectors.toList());
  }

  private List<Integer> getFreeBlocksIdsForExtension(int extension) {
    int count = (int) Math.ceil((double) extension / Block.MAX_BLOCK_SIZE);
    if (count > blocks.size()){
      return Collections.emptyList();
    }
    List<Integer> blocksIds = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Integer id = getFreeBlockId();
      if (id == -1) {
        blocksIds.forEach(bid->blocks.get(bid).setUsed(false));
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

  private int getDescriptorIndex(Descriptor descriptor){
    for (Entry<Integer, Descriptor> entry : descriptors.entrySet()) {
      if(entry.getValue().equals(descriptor)){
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

  private boolean checkName(String name) {
    return name != null && name.length() <= MAX_FILE_NAME_LENGTH && !directory.contains(name);
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
