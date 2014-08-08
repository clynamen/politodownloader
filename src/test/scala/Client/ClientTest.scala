package Client

import io.clynamen.github.PolitoDownloader.Client._
import org.eintr.loglady.Logging
import org.scalatest._


class ClientTest extends FunSpec with Logging {
  var userId : String = null
  var password : String = null

  // FIXME: Use BeforeAndAfterAll trait instead
  beforeAll()

  def beforeAll() {
   val credentials = getCredentials("userId", "password");
    log.info(f"userId: $userId%s password: $password%s")
  }

  def getCredentials(keys: String*) : Array[String] = {
    val getCleanedInput = (key : String)  => Console.readLine(f"Insert $key%s :\n").trim()
    keys.map(getCleanedInput).toArray
  }

  describe("Log in is ok") {
    val client = new Client(userId, password)

    client.reset()

    assert(!client.isUserLoggedIn())

    client.tryLogin()

    it("should be logged in") {
      assert(client.isUserLoggedIn())
    }
  }
}

