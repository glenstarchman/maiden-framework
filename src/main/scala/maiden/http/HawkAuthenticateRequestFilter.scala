package maiden.http

import cats.data.Xor
import cats.data.Xor._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import maiden.http.RequestContextBuilder.buildContext
import maiden.util.error.{AuthenticationFailedError, FinchTemplateError}
import maiden.util.hawk.HawkAuthenticate._
import maiden.util.hawk._
import maiden.util.hawk.validate.Credentials

abstract class HawkAuthenticateRequestFilter(credentials: Credentials) extends SimpleFilter[Request, Response] {
  override def apply(request: Request, service: Service[Request, Response]): Future[Response] =
    authenticate(request).fold(e => Future.exception(e), _ => service(request))

  private def authenticate(request: Request): Xor[FinchTemplateError, RequestValid] = {
    val valid = buildContext(request).map(context => authenticateRequest(credentials, context)).getOrElse(errorXor(s"Missing authentication header '$AuthorisationHttpHeader'"))
    valid.leftMap(e => new AuthenticationFailedError("Request is not authorised", Some(e)))
  }

  def notAuthorised[T](message: String): Xor[FinchTemplateError, T] = left(new AuthenticationFailedError(message))
}


