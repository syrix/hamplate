package hamplate

sealed abstract class Token {
  def content: String
}

sealed abstract class Matcher {
  /**
   * Returns the remaining String and the token if it matches this string or None if it doesn't match
   */
  def matches: PartialFunction[List[Char], (Token, String)]
}

class Intendation extends Token {
  val content = Intendation.INTEND.mkString
}
object Intendation extends Matcher {
  override def matches = {
    case ' ' :: ' ' :: x => (new Intendation, x.mkString)
  }

  val INTEND = ' ' :: ' ' :: Nil
}

case class LineType(content: String) extends Token
object LineType extends Matcher {
  // TODO SB allow class and id
  val knownTypes = Seq("%", ".", "#", ":")
  override def matches = {
    case x :: xs if knownTypes.contains(x + "") => (new LineType(x + ""), xs.mkString(""))
  }
}

case class RestOfLine(content: String) extends Token
object RestOfLine extends Matcher {
  override def matches = {
    case s => (new RestOfLine(s.mkString), "")
  }
}

class Newline extends Token {
  val content = "\n"
}

object Tokenizer {

  private def makeToken(line: String): List[Token] = {
    val (token, string) = (Intendation.matches orElse LineType.matches orElse RestOfLine.matches)(line.toList)
    if (string.isEmpty) token match {
      case RestOfLine(_) => List(token)
      // needed to throw an error on lines like "  #"
      case _ => List(token, new RestOfLine(""))
    }
    else token :: makeToken(string)
  }

  /**
   * Convert a list of lines to a list of tokens
   */
  def tokenize(lines: Seq[String]): Seq[Token] = {
    val tokensByLine = for (l <- lines) yield makeToken(l)

    // fold and remove first newline
    if (tokensByLine.isEmpty) Seq[Token](new Newline)
    else tokensByLine.foldLeft(Seq[Token]())((a, b) => a ++ Seq(new Newline) ++ b).tail ++ Seq(new Newline)
  }
}