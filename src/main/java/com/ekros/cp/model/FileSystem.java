package com.ekros.cp.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
public class FileSystem implements Serializable {

  public static final transient int MAX_DESCRIPTORS = 5;
  public static final transient int MAX_BLOCKS = 10;
  public static final transient int MAX_LINKS_AMOUNT = 3;
  public static final transient int MAX_FILE_NAME_LENGTH = 10;

  private final Map<Integer, Descriptor> fileDescriptors;
  private final List<Block> blocks;
  private final Descriptor directory;
  private static final transient Map<Integer, Descriptor> openFiles = new HashMap<>();

  public FileSystem() {
    directory = new Descriptor(true, null, null, 0);
    fileDescriptors = new HashMap<>();
    blocks = new ArrayList<>();
    init();
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
    Descriptor descriptor = new Descriptor(false, blockId, name, Block.MAX_BLOCK_SIZE);
    int index = getFreeIndex(fileDescriptors);
    directory.getLinks().add(index);
    fileDescriptors.put(index, descriptor);
    return true;
  }

  public void write(int fd, int offset, int size){
    if(!openFiles.containsKey(fd)){
      Log.error("Incorrect fd [" + fd + "]");
    }
    String data = getFileData(fd);
    if(size > data.length() || offset > data.length() || size+offset > data.length()){
      Log.error("Incorrect offset or size");
    }
    data = data.substring(0, offset) + "1".repeat(size) + data.substring(offset+size);
    int dataOffset = 0;
    for(Block block: getFileBlocks(openFiles.get(fd))) {
        block.setData(data.substring(dataOffset, dataOffset + Block.MAX_BLOCK_SIZE));
        dataOffset += Block.MAX_BLOCK_SIZE;
    }
  }

  public String read(int fd, int offset, int size) {
    if(!openFiles.containsKey(fd)){
      return "Incorrect fd";
    }
    String data = getFileData(fd);
    if(size > data.length() || offset > data.length()){
      return "Incorrect offset or size";
    }
    return data.substring(offset, offset+size);
  }

  public boolean truncate(String name, int size) {
    DescriptorMetadata metadata = findDescriptorByName(name);
    if (metadata == null || size < 0) {
      return false;
    }
    Descriptor descriptor = metadata.getDescriptor();
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

  public boolean openFile(String name) {
    DescriptorMetadata descriptorMetadata = findDescriptorByName(name);

    if (descriptorMetadata == null) {
      return false;
    }
    int index = getFreeIndex(openFiles);
    openFiles.put(index, descriptorMetadata.getDescriptor());
    Log.info("File [" + name + "] open with fd " + index);
    return true;
  }

  private String getFileData(int fd) {
    Descriptor descriptor = openFiles.get(fd);
    List<String> dataSet = descriptor.getBlockLinks().stream()
        .map(link -> blocks.get(link).getData()).collect(Collectors.toList());
    StringBuilder sb = new StringBuilder();
    dataSet.forEach(sb::append);
    return sb.toString();
  }

  public boolean link(String name1, String name2) {
    DescriptorMetadata descriptor = findDescriptorByName(name1);
    if (descriptor == null || !checkName(name2)) {
      return false;
    }
    descriptor.getDescriptor().addName(name2);
    return true;
  }

  public boolean unlink(String name) {
    DescriptorMetadata descriptorMetadata = findDescriptorByName(name);
    if (descriptorMetadata == null) {
      return false;
    }
    Descriptor descriptor = descriptorMetadata.getDescriptor();

    if (descriptor.getNameLinks().size() > 1) {
      descriptor.getNameLinks().remove(name);
      return true;
    }

    fileDescriptors.remove(descriptorMetadata.getKey());
    directory.getLinks().remove(descriptorMetadata.getKey());

    List<Integer> descriptorFd = new ArrayList<>();
    if (openFiles.containsValue(descriptor)) {
      openFiles.forEach((key, value) -> {
        if (value.equals(descriptor)) {
          descriptorFd.add(key);
        }
      });
    }
    descriptorFd.forEach(openFiles::remove);
    return true;
  }

  private List<Block> getFileBlocks(Descriptor descriptor) {
    return descriptor.getBlockLinks().stream().map(blocks::get).collect(Collectors.toList());
  }

  private DescriptorMetadata findDescriptorByName(String name) {
    return directory.getLinks().stream()
        .map(index -> new DescriptorMetadata(index, fileDescriptors.get(index)))
        .filter(desc -> desc.getDescriptor().getNameLinks().contains(name))
        .findFirst()
        .orElse(null);
  }

  public boolean closeFile(int fd) {
    return openFiles.remove(fd) != null;
  }

  public String getLinksInfo() {
    StringBuilder linksInfo = new StringBuilder("\n");
    fileDescriptors.forEach((key, value) -> value.getNameLinks()
        .forEach(fValue -> linksInfo.append(key).append(":").append(fValue).append("\n")));
    return linksInfo.toString();
  }

  public void format(int n) {
    if (n > fileDescriptors.size()) {
      Log.error("Incorrect size [" + n + "] max value: " + fileDescriptors.size());
      return;
    }

    Log.info("Descriptors before format: " + fileDescriptors.keySet());

    Iterator<Integer> iterator = directory.getLinks().listIterator();

    for (int i = 0; i < n && iterator.hasNext(); i++) {
      Integer key = iterator.next();
      getFileBlocks(fileDescriptors.get(key)).forEach(block -> block.setUsed(false));
      fileDescriptors.remove(key);
      iterator.remove();
    }

    Log.info("Descriptors after format: " + fileDescriptors.keySet());

  }

  private int getFreeIndex(Map<Integer, Descriptor> map) {
    for (int i = 0; i < map.size(); i++) {
      if (map.get(i) == null) {
        return i;
      }
    }
    return map.size();
  }

  private Integer getFreeBlockId() {
    return blocks.indexOf(blocks.stream()
        .filter(block -> !block.isUsed())
        .findFirst().orElse(null));
  }

  private boolean checkName(String name) {
    if (name == null || name.length() > MAX_FILE_NAME_LENGTH) {
      return false;
    }
    for (int i : directory.getLinks()) {
      Descriptor descriptor = fileDescriptors.get(i);
      if (descriptor.getNameLinks().contains(name)) {
        Log.error("Name " + name + " already exist.");
        return false;
      }
    }
    return true;
  }

  private boolean isDescriptorsMax() {
    return MAX_DESCRIPTORS == fileDescriptors.size();
  }

  @Getter
  @AllArgsConstructor
  private static class DescriptorMetadata {

    private Integer key;
    private Descriptor descriptor;
  }
}
