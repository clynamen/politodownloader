package Client

import io.clynamen.github.PolitoDownloader.Client._
import io.clynamen.github.PolitoDownloader.Utils.FileUtils
import io.clynamen.github.PolitoDownloader.Client.Parser._
import org.scalatest._

class ParserTest extends FunSuite {
  val nextLevelTestString = "javascript:nextLevel('12345','54321','12345');"
  val showIncTestString = "javascript:showInc('2014','D','1','1234');"

  def getTestFileContent(filename: String) = FileUtils.slurp(getClass.getResource(filename))

  test (f"nextLevel regex should match the string $nextLevelTestString") {
    val nextLevelRegex (inc, nod, doc) = nextLevelTestString
    assert(inc.toInt === 12345)
    assert(nod.toInt === 54321)
    assert(doc.toInt === 12345)
  }

  test (f"showInc regex should match the string $showIncTestString") {
    val showIncRegex(anno, tipo, int, mat) = showIncTestString
    assert(anno.toInt === 2014)
    assert(tipo === "D")
    assert(int.toInt === 1)
    assert(mat.toInt === 1234)
  }

  test("Parsed courses index should have 2 results") {
    val filename = "/lists/sviluppo.materiale.elenco.htm"
    val content : String = getTestFileContent(filename)
    assert(content != null)

    val parser = new Parser()
    val classes = parser.parseCourses(content)

    assert(2 === classes.length)
  }

  test("Parsed course index should have 3 results ") {
    val filename = "/lists/course.htm"
    val content : String = getTestFileContent(filename)
    assert(content != null)

    val parser = new Parser()
    val classes = parser.parseDirContent(content)

    assert(3 === classes.length)
  }

  test("Parsed video lessons page should have 3 results ") {
    val filename = "/lists/videolessons.htm"
    val content : String = getTestFileContent(filename)
    assert(content != null)

    val parser = new Parser()
    val videos = parser.parseVideoCoursePage(content)

    assert(3 === videos.length)
  }

  test("Parsed video courses page should have 4 results ") {
    val filename = "/lists/videocourses.htm"
    val content : String = getTestFileContent(filename)
    assert(content != null)

    val parser = new Parser()
    val videoCourses = parser.parseVideoCoursesListPage(content)

    assert(5 === videoCourses.length)
    print(videoCourses(0))
  }

}
