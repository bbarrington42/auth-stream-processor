package com

package object cda {

  def asString(thr: Throwable): String =
    s"${thr.getClass.getName}: ${thr.getMessage}}"

}
