# Source screen coverage

The Android navigation uses route arguments and sheets where the source uses Expo Router routes and modals. This map records where to review each source screen; it is not visual sign-off.

| Expo screen | Kotlin entry | Check coverage |
| --- | --- | --- |
| `welcome` | `Onboarding` | Welcome, number, demo verification, address, VIP and reward in emulator journeys |
| `(tabs)/index` | `HomeScreen` | Home capture, offer sheet and featured milk route |
| `(tabs)/shop` | `ShopScreen` | Category, milk catalogue, product and quick-add journeys |
| `(tabs)/protein` | `ProteinScreen` / `ShopScreen` | Screen inventory and narrow-layout captures |
| `(tabs)/subs` | `SubscriptionsScreen` | Empty and active captures, pause, resume, edit and custom plan |
| `(tabs)/orders` | `OrdersScreen` | Cancelled order persistence; source sample cards embedded |
| `(tabs)/profile` | `ProfileScreen` | Screen inventory, account sheets, wallet, support and feedback entry |
| `product` | `ProductScreen` | Purchase and subscription modes; pack, disclosure and add-ons review remains |
| `milk-offer` | `MilkOffersScreen` | Onboarding entry; individual campaign visuals still require detailed comparison |
| `milk-plan` | `PlanScreen` | Custom weekday generation, plan estimate and checkout |
| `checkout` | `CheckoutScreen`, `OrderConfirmation` | Demo payment and wallet checkout |
| `order` | `OrderScreen`, `DeliveryProgressCard` | Order receipt, cancellation and delivery stages; physical delivery is simulated |
| `wallet` | `WalletScreen` | Demo recharge, bonus, cancellation refund and relaunch |
| `calendar` | `CalendarScreen` | Screen inventory, selected date route and delivery-benefit unit cases |
| `support` | `SupportScreen` | Source samples, request details, follow-up and resolution |
| `assistant` | `AssistantScreen` | Rule-engine tests; complete visual comparison remains |
| `nutrition` | `NutritionScreen`, `ScheduledNutrition` | Estimate unit tests and subscription screen captures |
| `information` | `InformationScreen` | Source copy ported; detailed visual comparison remains |
| `feedback-demo` | `FeedbackScreen`, `FeedbackSampleCards` | Rating, notes, revision and persisted pending outbox |
| Expo layouts and not found | `CountryApp` | Android-owned navigation; iOS-only layout excluded |

The source contains additional sheets, disclosures and animation states within these routes. See [PARITY.md](PARITY.md) for remaining acceptance work.
