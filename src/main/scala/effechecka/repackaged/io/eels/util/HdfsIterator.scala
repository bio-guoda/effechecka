package effechecka.repackaged.io.eels.util
// adapted from https://github.com/51zero/eel-sdk

import org.apache.hadoop.fs.{LocatedFileStatus, RemoteIterator}

object HdfsIterator {
  def apply(iterator: RemoteIterator[LocatedFileStatus]): Iterator[LocatedFileStatus] = new Iterator[LocatedFileStatus] {
    override def hasNext(): Boolean = iterator.hasNext()
    override def next(): LocatedFileStatus = iterator.next()
  }
}