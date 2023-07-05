package it.pagopa.interop.tenantprocess.common.readmodel

trait ReadModelQuery {
  def mapToVarArgs[A, B](l: Seq[A])(f: Seq[A] => B): Option[B] = Option.when(l.nonEmpty)(f(l))
}
