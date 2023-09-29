package fi.kimmoeklund.domain

trait CrudResource[R, F]:
  def form: F
  def resource: R

// resurssista formiin
// toteutus ///type classilla Converter ... 
//
// https://blog.rockthejvm.com/scala-3-extension-methods/
