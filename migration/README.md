# Screenshot provenance

- `source-*.png`: the original Expo / React Native app, captured for comparison. These are not native Android screenshots.
- `native-proof/current.png`: Kotlin / Jetpack Compose app captured directly from the Android emulator.
- `native-proof/qa/*.png`: Kotlin emulator journey captures. Names describe the intended checkpoint; animated transitions can affect a capture, so inspect the image before using it as acceptance evidence.

Screenshots prove only the visible state. See PARITY.md for remaining comparison work. Device screenshots may contain demo session data.

See [the source route map](ROUTES.md) for screen locations and test coverage. Captures prefixed `narrow-` use 360dp width and 130% system text.
