package Client

import io.clynamen.github.PolitoDownloader.Client._
import io.clynamen.github.PolitoDownloader.Utils.FileUtils
import io.clynamen.github.PolitoDownloader.Client.Parser._
import org.scalatest._
import TestUtils.TestUtils._

class ParserTest extends FunSuite {
  val nextLevelTestString = "javascript:nextLevel('197469','32346912','11415');"
  val showIncTestString = "javascript:showInc('2014','D','3','6996');"

  def getTestFileContent(filename: String) = FileUtils.slurp(getClass.getResource(filename))

  test (f"nextLevel regex should match the string $nextLevelTestString") {
    val nextLevelRegex (inc, nod, doc) = nextLevelTestString
    assert(inc.toInt === 197469)
    assert(nod.toInt === 32346912)
    assert(doc.toInt === 11415)
  }

  test (f"showInc regex should match the string $showIncTestString") {
    val showIncRegex(anno, tipo, int, mat) = showIncTestString
    assert(anno.toInt === 2014)
    assert(tipo === "D")
    assert(int.toInt === 3)
    assert(mat.toInt === 6996)
  }

//  test("Parsed courses index should have 196 results") {
//    val filename = "/sviluppo.materiale.elenco.htm"
//    val content : String = getTestFileContent(filename)
//    assert(content != null)
//
//    val parser = new Parser()
//    val classes = parser.parseCourses(content)
//
//    assert(196 === classes.length)
//  }
//
//  test("Parsed course index should have 4 results ") {
//    val filename = "/corso.htm"
//    val content : String = getTestFileContent(filename)
//    assert(content != null)
//
//    val parser = new Parser()
//    val classes = parser.parseClasses(content)
//
//    assert(3 === classes.length)
//  }


}
