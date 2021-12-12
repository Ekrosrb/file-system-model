package com.ekros.cp.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DescriptorTest {

  @Test
  public void testSetSize(){
    Descriptor descriptor = new Descriptor(false, 0, "test", Block.MAX_BLOCK_SIZE);
    assertEquals(Block.MAX_BLOCK_SIZE, descriptor.getSize());
    descriptor.setSize(Block.MAX_BLOCK_SIZE*5-13);
    assertEquals(Block.MAX_BLOCK_SIZE*4, descriptor.getSize());
  }

}