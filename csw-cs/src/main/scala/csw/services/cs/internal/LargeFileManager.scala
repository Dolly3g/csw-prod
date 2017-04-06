package csw.services.cs.internal

import java.io.{File, FileOutputStream}
import java.nio.file.{Files, Path, Paths}

import net.codejava.security.HashGeneratorUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LargeFileManager(settings: Settings) {

  def post(inFile: File): Future[String] = Future {
    val id= HashGeneratorUtils.generateSHA1(inFile)
    val fileName = inFile.getName
    val dir = settings.dir.replaceFirst("~", System.getProperty("user.home"))

    val path = makePath(new File(dir), inFile)
    val outFile = path.toFile
    outFile.getParentFile.mkdirs()

    if (outFile.exists) {
      id
    }
    else {
      val out = new FileOutputStream(outFile)
      Files.copy(inFile.toPath, out)
      out.close()
      if(FileUtils.validate(fileName, outFile)) {
        id
      }
      else {
        outFile.delete()
        throw new RuntimeException(s" Error in creating file $id")
      }
    }
  }

  private def makePath(dir: File, file: File): Path = {
    val (subdir, name) = file.getName.splitAt(2)
    Paths.get(dir.getPath, subdir, name)
  }
}