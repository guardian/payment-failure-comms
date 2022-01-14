package payment_failure_comms

import okhttp3.OkHttpClient

import java.util.concurrent.TimeUnit.SECONDS

object HttpClient {

  private val client = new OkHttpClient.Builder()
    .connectTimeout(60, SECONDS)
    .readTimeout(60, SECONDS)
    .build()

  def apply(): OkHttpClient = client
}
