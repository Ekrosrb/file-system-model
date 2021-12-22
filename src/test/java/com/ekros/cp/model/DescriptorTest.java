package com.ekros.cp.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DescriptorTest {

  @Test
  public void testSetSize(){
    Descriptor descriptor = new Descriptor(false, false, 0, Block.MAX_BLOCK_SIZE, null, null);
    assertEquals(Block.MAX_BLOCK_SIZE, descriptor.getSize());
    descriptor.setSize(Block.MAX_BLOCK_SIZE*5-13);
    assertEquals(Block.MAX_BLOCK_SIZE*4, descriptor.getSize());
  }

}