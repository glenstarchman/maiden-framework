package maiden.util.hawk.parse

import maiden.util.hawk.TaggedTypesFunctions._
import maiden.util.hawk._
import maiden.util.hawk.parse.HeaderKeyValueParser.parseKeyValue
import maiden.util.hawk.validate.{MAC, RequestAuthorisationHeader}
import maiden.util.time.Time.parseSecondsAsTimeUtc

object RequestAuthorisationHeaderParser {
  def parseAuthHeader(header: RawAuthenticationHeader): Option[RequestAuthorisationHeader] =
    if (header.startsWith(s"$HawkHeaderValuePrefix ")) {
      parseSupportedHeader(header)
    } else {
      None
    }

  private def parseSupportedHeader(header: RawAuthenticationHeader): Option[RequestAuthorisationHeader] = {
    val kvs = parseKeyValues(header)
    for {
      id <- kvs.get(HeaderKey("id"))
      timestamp <- kvs.get(HeaderKey("ts")).flatMap(parseSecondsAsTimeUtc)
      nonce <- kvs.get(HeaderKey("nonce"))
      mac <- kvs.get(HeaderKey("mac"))
    } yield {
      val payloadHash = kvs.get(HeaderKey("hash")).map(PayloadHash)
      val extendedData = kvs.get(HeaderKey("ext")).map(ExtendedData)
      new RequestAuthorisationHeader(KeyId(id), timestamp, Nonce(nonce), payloadHash, extendedData, MAC(Base64Encoded(mac)))
    }
  }

  private def parseKeyValues(header: RawAuthenticationHeader): Map[HeaderKey, HeaderValue] = {
    val kvs = header.split(",").map(kv => parseKeyValue(HeaderKeyValue(kv)))
    kvs.foldLeft(Map[HeaderKey, HeaderValue]())((acc, maybeKv) => acc ++ maybeKv.getOrElse(Map()))
  }
}
