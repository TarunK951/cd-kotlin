# Engineering handoff

The app uses Kotlin, Jetpack Compose, and Navigation Compose. Screens collect lifecycle-aware state from the app models.

The connected shopping flow loads products, orders, subscriptions, and feedback through the app API. Android Keystore protects the saved sign-in token. Checkout and subscription recovery retain pending requests so an uncertain response can be checked before another submission.

The local test flow uses DataStore for session snapshots and packages a fixture catalog at `app/src/main/assets/catalogue.json`. The domain tests cover pricing, scheduling, and feedback eligibility. The Compose journeys exercise shopping and checkout recovery on an Android device or emulator.

The debug application ID is `com.countryd.app.nativepreview`. The release application ID is `com.countryd.app`.

Run `node tools/check-assets.cjs` to verify packaged asset checksums and the fixture catalog.
