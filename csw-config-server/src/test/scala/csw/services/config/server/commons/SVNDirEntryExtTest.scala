package csw.services.config.server.commons

import java.util.Date
import java.util.regex.Pattern

import csw.services.config.server.{ServerWiring, Settings}
import csw.services.config.server.commons.SVNDirEntryExt.RichSvnDirEntry
import org.scalatest.{FunSuite, Matchers}
import org.tmatesoft.svn.core.{SVNDirEntry, SVNNodeKind}

class SVNDirEntryExtTest extends FunSuite with Matchers {
  val settings: Settings = new ServerWiring().settings

  test("should match the pattern for relative path") {
    val dirEntry = new SVNDirEntry(settings.svnUrl, settings.svnUrl, "a/b/sample.txt", SVNNodeKind.FILE, 100, false, 1,
      new Date(), "author", "comment")

    val patterns = List(
      Pattern.compile("a/"),
      Pattern.compile("a/b"),
      Pattern.compile("a/b/sample"),
      Pattern.compile("/b/sample"),
      Pattern.compile("sample"),
      Pattern.compile(".txt"),
      Pattern.compile("sample.txt"),
      Pattern.compile("")
    )

    dirEntry.isFile shouldBe true
    dirEntry.isNotDefault(settings.`default-suffix`) shouldBe true

    patterns.foreach(pattern ⇒ dirEntry.matches(Some(pattern)) shouldBe true)
  }

  test("should not match invalid pattern for relative path") {
    val dirEntry = new SVNDirEntry(settings.svnUrl, settings.svnUrl, "a/b/sample.txt", SVNNodeKind.FILE, 100, false, 1,
      new Date(), "author", "comment")

    val pattern = Pattern.compile("invalidstring")
    dirEntry.matches(Some(pattern)) shouldBe false
  }
}