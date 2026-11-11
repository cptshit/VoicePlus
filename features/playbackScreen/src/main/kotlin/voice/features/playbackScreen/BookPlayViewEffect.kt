package voice.features.playbackScreen

internal sealed interface BookPlayViewEffect {
  data object RequestIgnoreBatteryOptimization : BookPlayViewEffect
}
