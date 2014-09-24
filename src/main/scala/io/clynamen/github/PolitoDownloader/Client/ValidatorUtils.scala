package io.clynamen.github.PolitoDownloader.Client

object ValidatorUtils {
  def useFirstValid[T, R](validators: Iterable[T =>Option[R]], value: T) : Option[R] = {
    for(validator <- validators) {
      val result = validator(value)
      if(result.isDefined) return result
    }
    None
  }

  def applyValidatorsAndReturnOnlyValid[T, R](validators: Iterable[T => Option[R]],
                                             values: Iterable[T]) : Iterable[R] = {
    values.map(v=> useFirstValid(validators, v)).filter(r => r.isDefined).map(r => r.get)
  }
}
