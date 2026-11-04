# Migration parity

The requirement is to preserve the existing app, including assets, screens, states, layout and motion. The current native implementation is not yet signed off against that requirement.

| Source area | Native implementation | Current boundary |
| --- | --- | --- |
| Welcome, mobile, verification, address, unavailable area, VIP, reward | `Onboarding` and native motion components | Original illustration layers exported; native forms and validation implemented; detailed spacing and animation comparison still required |
| Home | Home, delivery overview, campaigns, categories and shelves | Main sections present; scroll-responsive chrome, overlays and smaller promotional states need further parity work |
| Shop and Protein | Category grid, search, local-name aliases, filters, product grids | Main catalogue flow implemented; continuous category sidebar implemented; search-loading/error QA states and visual comparison remain open |
| Product | Selected-pack panel, variants, buy-once/subscribe control, quality, nutrition and add-ons | Native implementation present; all disclosure, sheet and customisation states are not visually signed off |
| Milk offer and plan | Offer selection, quantity, schedule, dates and price estimate | Charging and date rules tested; paid/free benefit strip and original custom-weekday schedule implemented; detailed visual/motion comparison remains open |
| Basket and checkout | Cart lines, plan summary, instructions, tip, coupon, points, wallet and demo confirmation | Main flow implemented; native basket sheet and success dialog implemented; original Offers & extras icons and recommendation filters, carry-bag toggle, compact cart pill, wallet and coin artwork, delivery preferences with an inline date picker, and fixed Place order action in the cart sheet implemented; detailed motion comparison remains open |
| Orders and order details | Receipt, status, repeat, cancellation, help and delivery rating | Main actions implemented; original tracking van and stage copy, source order filters and inline sample deliveries implemented; detailed visual comparison remains open |
| Subscriptions | Active/paused/past, edit, skip, vacation and end | Main controls implemented; counted tabs and nutrition disclosures implemented; paid/free offer calendar, persistent disclosure and date deep links implemented; detailed visual comparison remains open |
| Calendar and nutrition | Date selection and scheduled nutrient estimates | Monthly delivered/upcoming comparison implemented; all calendar states still need visual acceptance |
| Wallet | Original recharge threshold and bonus, local balance, activity and autopay preference | Core behavior implemented; processing and completion states plus 22-particle / 1400ms burst implemented; detailed animation comparison remains open; autopay remains a local demo preference |
| Profile, addresses, wishlist, VIP | Native account and saved data screens | Main actions present; notification preferences implemented; exact settings-sheet presentation needs further comparison |
| Product feedback | Five policies, rating/issues/photo, local revisions and pending outbox | Core policy ported; prompt timing, reminder lifecycle and all reorder states need further parity work |
| Support | Local requests, follow-up, status and language | Main request lifecycle present; source sample cases, order previews, follow-up and status grouping implemented; detailed visual acceptance remains open |
| Delight assistant | Local conversation and navigation shortcuts | Source rule-based recommendations, basket, meal planning and monthly summaries implemented; detailed interaction and visual acceptance remain open |
| Motion and accessibility | Press scale, route fades, carousel, VIP and reward animation, system motion setting | Native equivalents exist; frame-by-frame equivalence, large text and assistive-technology review remain open |

All original assets are copied and hashed. This establishes asset preservation; it does not establish that every asset is already displayed in the same state as the source.

Native emulator journeys and calculation tests are recorded separately from visual parity. Production integrations remain simulated, as in the source app.

## Latest review pass

- Asset verifier: 253 original files match their SHA-256 hashes; 24 catalogue products load.
- Calculation tests: 31 passed.
- Navigation: main tabs now open their root, preventing a saved milk-offer or wallet detail screen from reopening under Home or More. Emulator regression checks are rerun after this change.
- Plan choices now show the source customer-facing labels instead of internal offer identifiers.
- Native screenshots are explicitly separated from React Native reference screenshots in README.md.

Remaining acceptance includes offer-calendar paid/free treatment, support sample states, detailed spacing, full motion comparison, narrower screens, large text and Android accessibility. Passing automated journeys does not close those items.

Final checkpoint for this pass: 31 calculation tests and all 3 Android emulator journeys passed. Debug APK assembles and lint passes (0 errors, 13 warnings). Full visual acceptance remains open.

## Follow-up comparison fixes

- Source bundle offers now have a persisted countdown, qualifying-pack progress, Add/Added state, coupon errors/removal, free-delivery threshold and automatic gift rows in the basket.
- Home milk cards use the two-product source layout and open product details in Subscribe mode. The fruit campaign uses the original peach palette, artwork and discount sticker. Welcome campaign is shared between the carousel and milk section.
- Delivery benefits distinguish Paid/Free, use a 14-day strip, open the chosen calendar date and retain expansion state. Orders linked to subscriptions no longer double the overview delivery count.
- Milk custom schedules use source weekday selection and 40 generated delivery dates; saved-address validation includes locality and custom labels.
- Support includes the three source sample orders/requests, stable references, order previews and escalation grouped under Open.
- VIP panel uses the source green membership summary, savings row, grouped benefits and details disclosure.
- Reduced motion covers button press scaling, route fades, campaign movement and celebrations.
- Native emulator captures include account, calendar, wishlist, addresses, VIP, routine, support, offer cards and basket gifts in `native-proof/qa`.

This work does not establish frame-by-frame identity. Full screenshot acceptance across every scroll position, every system font size and every Android device is not claimed. Live services remain simulated as in the source.
