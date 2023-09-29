package fi.kimmoeklund.domain

trait CrudResource[R, F]:
  def form: F
  def resource: R

