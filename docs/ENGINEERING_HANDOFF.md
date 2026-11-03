# Engineering handoff

## Implementation

The Android app is written in Kotlin. Compose renders every interactive screen. Navigation Compose owns the back stack. `CountryViewModel` exposes a `StateFlow`; screens collect it with lifecycle awareness. DataStore saves a complete session snapshot, including orders, wallet changes, subscriptions and feedback revisions. A single write queue preserves the order of those snapshots.

The `domain` package contains calculations that do not depend on Android. It ports the source app's catalogue-pack selection, subscription pack charging, free-pack rules, VIP savings, coupons, reward points, wallet recharge bonus, delivery cadence, skipped dates, vacation exclusions, nutrient estimates and feedback cooldown policy. Tests cover boundary cases rather than individual UI implementation details.

The catalogue comes from `src/mockData.ts`. Product IDs, names, prices, pack variants and add-ons are preserved in `catalogue.json`. All 253 files from the source `assets` directory are included under `app/src/main/assets/assets`; their hashes are recorded in `migration/source-manifest.json`. Onest font files are packaged as Android font resources. Lucide paths are native VectorDrawables, with their license in `third-party`.

First-use artwork was exported from the original rendered illustration layers at three times resolution. These exports contain artwork, not interactive controls. Compose supplies the inputs, buttons, layout and transitions. The original asset files are retained alongside the exports.

The debug application ID has a `.nativepreview` suffix. Release keeps `com.countryd.app`, but release signing credentials and store deployment are not configured.

## Source behavior retained

* Customer orders, plans and wallet start empty. Explicit demo checkout creates activity.
* A cancelled order refunds only the wallet contribution and restores used reward points; it does not end a recurring subscription.
* Checkout updates order, wallet, points, cart and plan state together.
* A new schedule replaces an earlier active schedule for the same product while retaining the earlier plan as history.
* Free gifts require catalogue-sized, one-time packs. Add-ons remain chargeable on subscription gift deliveries.
* Native feedback remains in a durable pending outbox. There is no browser bridge or backend acknowledgement in the Android app. Photo metadata is separate from the local content URI.
* Demo login, payments, support and delivery are labelled as such. No payment SDK, server credential or live endpoint is embedded.

## Review boundaries

This branch is not a claim of full one-to-one migration acceptance. `migration/PARITY.md` records the areas still requiring detailed comparison. Compiling, linting and passing selected journeys do not prove every visual state, gesture or animation matches.

The source has a much larger set of presentation components than routes. The native implementation currently groups those into fewer Compose components. Reviewers should use the parity notes and source hashes to assess equivalence, rather than matching file names.

Dependency versions are pinned to a compatible Java 17 / AGP 8.13 stack. Lint may report newer available dependencies; upgrades should be tested as a set. The repository does not suppress lint errors or use a generated lint baseline.

All source assets were retained at the user's request. Once parity is accepted, unused design/reference images can be excluded from the APK in a separate, reviewable size optimisation.
