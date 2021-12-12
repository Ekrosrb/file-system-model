package com.ekros.cp.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
public class Descriptor implements Serializable {
  private final boolean isDirectory;
  @Setter
  private int size;
  private final List<Integer> links;
  private final List<String> nameLinks;
  private final List<Integer> blockLinks;

  public Descriptor(boolean isDirectory, Integer blockLink, String name, int size){
    this.links = new ArrayList<>();
    this.blockLinks = new ArrayList<>();
    this.isDirectory = isDirectory;
    this.size = size;
    nameLinks = new ArrayList<>();
    if(blockLink != null){
      blockLinks.add(blockLink);
    }
    if(name != null){
      nameLinks.add(name);
    }
  }

  public void addName(String name){
    nameLinks.add(name);
  }

}
