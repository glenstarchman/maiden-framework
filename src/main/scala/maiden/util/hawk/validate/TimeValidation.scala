package maiden.util.hawk.validate

import cats.data.Xor
import com.github.benhutchison.mouse.all._
import maiden.util.hawk._
import maiden.util.hawk.params.RequestContext
import maiden.util.time.Time.nowUtc
import org.joda.time.Duration

trait TimeValid

object TimeValidation extends Validator[TimeValid] {
  val acceptableTimeDelta = Duration.standardMinutes(2)

  override def validate(credentials: Credentials, context: RequestContext, method: ValidationMethod): Xor[HawkError, TimeValid] = {
    val delta = nowUtc.minus(context.clientAuthHeader.timestamp).getStandardSeconds
    (delta <= acceptableTimeDelta.getStandardSeconds).xor(error("Timestamp invalid"), new TimeValid {})
  }
}
