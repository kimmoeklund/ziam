import fi.kimmoeklund.domain.User
case class RequestContext(requestId: String, user: Option[User]) 
