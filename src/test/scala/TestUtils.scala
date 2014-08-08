package TestUtils

object TestUtils {
  def getCleanedInput(key : String) : String = Console.readLine(f"Insert $key%s :\n").trim()
}

