package voice.features.tagReview.lookup

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Fetches a URL as text. Throws [IOException] when the service cannot be reached or answers with an error. */
fun interface CatalogHttp {
  suspend fun get(url: String): String
}

/** Names the app and its version, no email address and no device identifier. */
internal fun catalogUserAgent(
  versionName: String?,
  packageName: String,
): String = "VoicePlus/${versionName ?: "unknown"} ($packageName)"

/**
 * Own client on purpose: the cover search client sends a browser User-Agent, but MusicBrainz asks
 * applications to identify themselves, and wants at most one request per second.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class OkHttpCatalogHttp(context: Context) : CatalogHttp {

  private val client = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()
  private val userAgent = catalogUserAgent(
    versionName = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull(),
    packageName = context.packageName,
  )
  private val musicBrainzLock = Mutex()
  private var lastMusicBrainzRequest = 0L

  override suspend fun get(url: String): String = if (url.startsWith(MUSICBRAINZ)) {
    musicBrainzLock.withLock {
      val wait = lastMusicBrainzRequest + MIN_INTERVAL_MS - System.currentTimeMillis()
      if (wait > 0) delay(wait)
      try {
        execute(url)
      } finally {
        lastMusicBrainzRequest = System.currentTimeMillis()
      }
    }
  } else {
    execute(url)
  }

  private suspend fun execute(url: String): String = withContext(Dispatchers.IO) {
    val request = Request.Builder().url(url).header("User-Agent", userAgent).header("Accept", "application/json").build()
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
      response.body.string()
    }
  }

  private companion object {
    const val MUSICBRAINZ = "https://musicbrainz.org/"
    const val MIN_INTERVAL_MS = 1100L
  }
}
