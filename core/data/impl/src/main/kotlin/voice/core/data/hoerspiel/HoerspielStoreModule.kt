package voice.core.data.hoerspiel

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import voice.core.data.store.VoiceDataStoreFactory

/** Fork-only settings, kept out of the upstream StoreModule. */
@ContributesTo(AppScope::class)
public interface HoerspielStoreModule {

  @Provides
  @SingleIn(AppScope::class)
  @CoverStyleStore
  private fun coverStyle(factory: VoiceDataStoreFactory): DataStore<CoverStyle> {
    return factory.create(CoverStyle.serializer(), CoverStyle.Cd, "coverStyle", replaceCorruptedWithDefault = true)
  }
}
