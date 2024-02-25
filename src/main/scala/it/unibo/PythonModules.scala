package it.unibo

import me.shadaj.scalapy.py

object PythonModules {
  val utils = py.module("SCRPythonUtils")
  val torch = py.module("torch")
}
