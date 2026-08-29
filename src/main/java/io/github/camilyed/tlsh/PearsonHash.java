package io.github.camilyed.tlsh;

class PearsonHash {
  private final int[] permutation;

  PearsonHash(int[] permutation) {
    if (permutation.length != 256) {
      throw new IllegalArgumentException("Permutation must contain exactly 256 values");
    }
    this.permutation = permutation.clone();
  }

  int map(int salt, byte firstByte, byte secondByte, byte thirdByte) {
    int unsignedFirst = unsigned(firstByte);
    int h = permutation[salt];
    h = permutation[h ^ unsignedFirst];
    h = permutation[h ^ unsigned(secondByte)];
    h = permutation[h ^ unsigned(thirdByte)];
    return h;
  }

  private static int unsigned(byte firstByte) {
    return Byte.toUnsignedInt(firstByte);
  }
}
