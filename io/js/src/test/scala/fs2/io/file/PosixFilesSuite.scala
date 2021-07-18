/*
 * Copyright (c) 2013 Functional Streams for Scala
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package fs2
package io
package file

import cats.effect.IO
import cats.syntax.all._

import scala.concurrent.duration._
import fs2.internal.jsdeps.node.osMod
import cats.kernel.Order

class PosixFilesSuite extends Fs2Suite with BaseFileSuite {
  import PosixFiles._

  group("readAll") {
    test("retrieves whole content of a file") {
      Stream
        .resource(tempFile.evalMap(modify))
        .flatMap(path => PosixFiles[IO].readAll(path, 4096))
        .map(_ => 1)
        .compile
        .foldMonoid
        .assertEquals(4)
    }
  }

  // group("readRange") {
  //   test("reads half of a file") {
  //     Stream
  //       .resource(tempFile.evalMap(modify))
  //       .flatMap(path => PosixFiles[IO].readRange(path, 4096, 0, 2))
  //       .map(_ => 1)
  //       .compile
  //       .foldMonoid
  //       .assertEquals(2)
  //   }
  //   test("reads full file if end is bigger than file size") {
  //     Stream
  //       .resource(tempFile.evalMap(modify))
  //       .flatMap(path => PosixFiles[IO].readRange(path, 4096, 0, 100))
  //       .map(_ => 1)
  //       .compile
  //       .foldMonoid
  //       .assertEquals(4)
  //   }
  // }

  // group("writeAll") {
  //   test("simple write") {
  //     Stream
  //       .resource(tempFile)
  //       .flatMap { path =>
  //         Stream("Hello", " world!")
  //           .covary[IO]
  //           .through(text.utf8.encode)
  //           .through(PosixFiles[IO].writeAll(path)) ++ PosixFiles[IO]
  //           .readAll(path, 4096)
  //           .through(text.utf8.decode)
  //       }
  //       .compile
  //       .foldMonoid
  //       .assertEquals("Hello world!")
  //   }

  //   test("append") {
  //     Stream
  //       .resource(tempFile)
  //       .flatMap { path =>
  //         val src = Stream("Hello", " world!").covary[IO].through(text.utf8.encode)
  //         src.through(PosixFiles[IO].writeAll(path)) ++
  //           src.through(PosixFiles[IO].writeAll(path, OpenMode.O_APPEND)) ++ PosixFiles[IO]
  //             .readAll(path, 4096)
  //             .through(text.utf8.decode)
  //       }
  //       .compile
  //       .foldMonoid
  //       .assertEquals("Hello world!Hello world!")
  //   }
  // }

  // group("tail") {
  //   test("keeps reading a file as it is appended") {
  //     Stream
  //       .resource(tempFile)
  //       .flatMap { path =>
  //         PosixFiles[IO]
  //           .tail(path, 4096, pollDelay = 25.millis)
  //           .concurrently(modifyLater(path))
  //       }
  //       .take(4)
  //       .map(_ => 1)
  //       .compile
  //       .foldMonoid
  //       .assertEquals(4)
  //   }
  // }

  // group("exists") {
  //   test("returns false on a non existent file") {
  //     PosixFiles[IO].access(Path("nothing")).assertEquals(false)
  //   }
  //   test("returns true on an existing file") {
  //     tempFile
  //       .use(PosixFiles[IO].access(_))
  //       .assertEquals(true)
  //   }
  // }

  // group("permissions") {
  //   test("should fail for a non existent file") {
  //     PosixFiles[IO]
  //       .stat(Path("nothing"))
  //       .intercept[Throwable]
  //   }
  //   test("should return permissions for existing file") {
  //     val permissions =
  //       FileAccessMode.S_IRWXU | FileAccessMode.S_IRWXG | FileAccessMode.S_IROTH | FileAccessMode.S_IXOTH
  //     tempFile
  //       .use { p =>
  //         PosixFiles[IO].chmod(p, permissions) >>
  //           PosixFiles[IO].stat(p).map(_.mode.access)
  //       }
  //       .assertEquals(permissions)
  //   }
  // }

  // group("setPermissions") {
  //   test("should fail for a non existent file") {
  //     PosixFiles[IO]
  //       .chmod(Path("nothing"), FileAccessMode.Default)
  //       .intercept[Throwable]
  //   }
  //   test("should correctly change file permissions for existing file") {
  //     val permissions =
  //       FileAccessMode.S_IRWXU | FileAccessMode.S_IRWXG | FileAccessMode.S_IROTH | FileAccessMode.S_IXOTH
  //     tempFile
  //       .use { p =>
  //         for {
  //           initialPermissions <- PosixFiles[IO].stat(p).map(_.mode.access)
  //           _ <- PosixFiles[IO].chmod(p, permissions)
  //           updatedPermissions <- PosixFiles[IO].stat(p).map(_.mode.access)
  //         } yield {
  //           assertNotEquals(initialPermissions, updatedPermissions)
  //           assertEquals(updatedPermissions, permissions)
  //         }
  //       }
  //   }
  // }

  // group("copy") {
  //   test("returns a path to the new file") {
  //     (tempFile, tempDirectory).tupled
  //       .use { case (filePath, tempDir) =>
  //         val newFile = tempDir / Path("newfile")
  //         PosixFiles[IO]
  //           .copyFile(filePath, newFile) >> PosixFiles[IO].access(newFile)
  //       }
  //       .assertEquals(true)
  //   }
  // }

  // group("deleteIfExists") {
  //   test("should result in non existent file") {
  //     tempFile
  //       .use { path =>
  //         PosixFiles[IO].rm(path) >> PosixFiles[IO].access(path)
  //       }
  //       .assertEquals(false)
  //   }
  // }

  // group("delete") {
  //   test("should fail on a non existent file") {
  //     PosixFiles[IO]
  //       .rm(Path("nothing"))
  //       .intercept[Throwable]
  //   }
  // }

  // group("deleteDirectoryRecursively") {
  //   test("should remove a non-empty directory") {
  //     val testPath = Path("a")
  //     PosixFiles[IO].mkdir(testPath / Path("b/c"), recursive = true) >>
  //       PosixFiles[IO].rm(testPath, recursive = true) >>
  //       PosixFiles[IO].access(testPath).assertEquals(false)
  //   }
  // }

  // group("move") {
  //   test("should result in the old path being deleted") {
  //     (tempFile, tempDirectory).tupled
  //       .use { case (filePath, tempDir) =>
  //         PosixFiles[IO].rename(filePath, tempDir / Path("newfile")) >>
  //           PosixFiles[IO].access(filePath)
  //       }
  //       .assertEquals(false)
  //   }
  // }

  // group("size") {
  //   test("should return correct size of ay file") {
  //     tempFile
  //       .use { path =>
  //         modify(path) >> PosixFiles[IO].stat(path).map(_.size)
  //       }
  //       .assertEquals(4L)
  //   }
  // }

  // group("tempDirectoryStream") {
  //   test("should remove the directory following stream closure") {
  //     Stream
  //       .resource {
  //         PosixFiles[IO]
  //           .mkdtemp(Path(""))
  //           .evalMap(path => PosixFiles[IO].access(path).tupleRight(path))
  //       }
  //       .compile
  //       .lastOrError
  //       .flatMap { case (existsBefore, path) =>
  //         PosixFiles[IO]
  //           .access(path)
  //           .tupleLeft(existsBefore)
  //           .assertEquals(true -> false)
  //       }
  //   }

  //   test("should not fail if the directory is deleted before the stream completes") {
  //     Stream
  //       .resource(PosixFiles[IO].mkdtemp(Path("")))
  //       .evalMap(PosixFiles[IO].rmdir(_))
  //       .compile
  //       .lastOrError
  //   }

  //   test("should create the directory in the specified directory") {
  //     tempDirectory
  //       .use { tempDir =>
  //         val files = PosixFiles[IO]
  //         files
  //           .mkdtemp(tempDir)
  //           .use { directory =>
  //             files.access(tempDir / directory.basename)
  //           }
  //       }
  //       .assertEquals(true)
  //   }

  //   test("should create the directory in the default temp directory when dir is not specified") {

  //     val files = PosixFiles[IO]

  //     files
  //       .mkdtemp()
  //       .use { directory =>
  //         IO(Path(osMod.tmpdir())).flatMap(dir => files.access(dir / directory.basename))
  //       }
  //       .assertEquals(true)
  //   }
  // }

  // group("createDirectory") {
  //   test("should return in an existing path") {
  //     tempDirectory
  //       .use { path =>
  //         PosixFiles[IO]
  //           .mkdir(path / Path("temp"))
  //           .bracket(PosixFiles[IO].access(_))(PosixFiles[IO].rmdir(_).void)
  //       }
  //       .assertEquals(true)
  //   }
  // }

  // group("createDirectories") {
  //   test("should return in an existing path") {
  //     tempDirectory
  //       .use { path =>
  //         PosixFiles[IO]
  //           .mkdir(path / Path("temp/inner"), recursive = true)
  //           .bracket(PosixFiles[IO].access(_))(PosixFiles[IO].rmdir(_).void)
  //       }
  //       .assertEquals(true)
  //   }
  // }

  // group("directoryStream") {
  //   test("returns an empty Stream on an empty directory") {
  //     tempDirectory
  //       .use { path =>
  //         PosixFiles[IO]
  //           .opendir(path)
  //           .compile
  //           .last
  //       }
  //       .assertEquals(None)
  //   }

  //   test("returns all files in a directory correctly") {
  //     Stream
  //       .resource(tempFiles(10))
  //       .flatMap { paths =>
  //         val parent = paths.head.dirname
  //         PosixFiles[IO]
  //           .opendir(parent)
  //           .map(path => paths.exists(_.normalize == path.normalize))
  //       }
  //       .compile
  //       .fold(true)(_ & _)
  //       .assertEquals(true)
  //   }
  // }

  // group("walk") {
  //   test("returns the only file in a directory correctly") {
  //     Stream
  //       .resource(tempFile)
  //       .flatMap { path =>
  //         PosixFiles[IO].walk(path.dirname).map(_.normalize == path.normalize)
  //       }
  //       .map(_ => 1)
  //       .compile
  //       .foldMonoid
  //       .assertEquals(2) // the directory and the file
  //   }

  //   test("returns all files in a directory correctly") {
  //     Stream
  //       .resource(tempFiles(10))
  //       .flatMap { paths =>
  //         val parent = paths.head.dirname
  //         PosixFiles[IO]
  //           .walk(parent)
  //           .map(path => (parent :: paths).exists(_.normalize == path.normalize))
  //       }
  //       .compile
  //       .fold(true)(_ & _)
  //       .assertEquals(true) // the directory itself and the files
  //   }

  //   test("returns all files in a nested tree correctly") {
  //     Stream
  //       .resource(tempFilesHierarchy)
  //       .flatMap(topDir => PosixFiles[IO].walk(topDir))
  //       .map(_ => 1)
  //       .compile
  //       .foldMonoid
  //       .assertEquals(31) // the root + 5 children + 5 files per child directory
  //   }
  // }

  // test("writeRotate") {
  //   val bufferSize = 100
  //   val totalBytes = 1000
  //   val rotateLimit = 150
  //   Stream
  //     .resource(tempDirectory)
  //     .flatMap { dir =>
  //       Stream.eval(IO.ref(0)).flatMap { counter =>
  //         val path = counter.modify(i => (i + 1) -> dir / Path(i.toString))

  //         val write = Stream(0x42.toByte).repeat
  //           .buffer(bufferSize)
  //           .take(totalBytes.toLong)
  //           .through(PosixFiles[IO].writeRotate(path, rotateLimit.toLong))

  //         val verify = PosixFiles[IO]
  //           .opendir(dir)
  //           .evalMap { path =>
  //             PosixFiles[IO].stat(path).map(_.size).tupleLeft(path)
  //           }

  //         write ++ verify
  //       }
  //     }
  //     .compile
  //     .toList
  //     .map { results =>
  //       val sizes = results
  //         .sortBy { case (path, _) => path }(Order[Path].toOrdering)
  //         .map { case (_, size) => size }

  //       assertEquals(sizes.size, (totalBytes + rotateLimit - 1) / rotateLimit)
  //       assert(sizes.init.forall(_ == rotateLimit))
  //       assertEquals(sizes.last, (totalBytes % rotateLimit).toLong)
  //     }
  // }

  // group("isDirectory") {
  //   test("returns false if the path is for a file") {
  //     tempFile
  //       .use(PosixFiles[IO].stat(_).map(_.isDirectory))
  //       .assertEquals(false)
  //   }

  //   test("returns true if the path is for a directory") {
  //     tempDirectory
  //       .use(PosixFiles[IO].stat(_).map(_.isDirectory))
  //       .assertEquals(true)
  //   }
  // }

  // group("isFile") {
  //   test("returns true if the path is for a file") {
  //     tempFile
  //       .use(PosixFiles[IO].stat(_).map(_.isFile))
  //       .assertEquals(true)
  //   }

  //   test("returns false if the path is for a directory") {
  //     tempDirectory
  //       .use(PosixFiles[IO].stat(_).map(_.isFile))
  //       .assertEquals(false)
  //   }
  // }
}
