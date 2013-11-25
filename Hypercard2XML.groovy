import org.apache.commons.io.IOUtils

def dir = new File(args[0])
def dest = new File(dir.parent, 'exported')
dest.mkdirs()

dir.eachFile { orig ->
  orig.withInputStream { is ->
    byte[] bytes = IOUtils.toByteArray(is)
    new File(dest, orig.getName() + "_exported.xml").withWriter('UTF8') { w ->
      def builder = new groovy.xml.MarkupBuilder(w)
      StringBuffer buf = new StringBuffer()
      int i = 0;
      int numberOfCards = 0

      if (readLong(i + 4, bytes, 4) == 0x5354414b /* STAK */) {
        numberOfCards = readLong(i + 40, bytes, 4); i += readLong(i, bytes, 4) /*Skip Stack block*/
      }

      def toSkip = [0x4d415354 /* MAST */ as long, 0x4c495354 /* LIST */ as long, 0x50414745 /* PAGE */ as long, 0x424b4744 /* BKGD */ as long, 0x424d4150 /* BMAP */ as long]
      while (toSkip.contains(readLong(i + 4, bytes, 4))) {
        i += readLong(i, bytes, 4) /* Skip */
      }

      builder.stack(cards: numberOfCards) {
        while (readLong(i + 4, bytes, 4) == 0x43415244 /* CARD */) {
          nextI = i + readLong(i, bytes, 4)
          long id = readLong(i + 8, bytes, 4)
          long nbrParts = readLong(i + 40, bytes, 2)
          long nbrPartContents = readLong(i + 48, bytes, 2)

          i += 54

          //Skip parts... Might want to do something with them
          for (int j = 0; j < nbrParts; j++) {
            i += readLong(i, bytes, 2)
          }

          card(id: id, contents: nbrPartContents) {
            for (int j = 0; j < nbrPartContents; j++) {
              int pl = readLong(i + 2, bytes, 2)
              content(id: readLong(i, bytes, 2), getPartContent(i + 4, bytes, pl))
              i += 2 + 2 + pl + (pl % 2 == 1 ? 1 : 0)
            }
          }
          i = nextI
        }
      }
    }
  }
}

String getPartContent(int offset, byte[] bytes, length) {
  if (bytes[offset] == 0) {
    return new String(bytes, offset + 1, length - 1, 'MacRoman')
  } else {
    int sl = readLong(offset, bytes, 2) - 32770
    return new String(bytes, offset + 2 + sl, length - sl - 2, 'MacRoman')
  }
}

long readLong(int offset, byte[] bytes, int length) {
  long r = 0
  for (int j = 0; j < length; j++) {
    r = r * 256 + (bytes[offset + j] & 0xff)
  }
  return r;
}
