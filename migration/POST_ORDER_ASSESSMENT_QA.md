# Post-delivery feedback assessment checks

Source references are the supplied assessment brief, the Figma feedback flow, and API integration guide v5.

| Scenario | Expected result | Evidence |
| --- | --- | --- |
| Subscribed product | Automatic card stays hidden. A paused subscription also counts as subscribed. | Prior live fixture checks returned no prompt. |
| Repeat purchase before rating | One eligible product prompt, with feedback tied to the delivered item. | Fixture 103 returned one Paneer prompt. |
| Product waiting period and frequency cap | Home displays a card only when the server returns show=true. | Waiting-period fixture returned no prompt. Frequency-cap fixture 104 returned no prompt. |
| Skip | Card closes after successful skip. Automatic eligibility is decided by the server. | Skip endpoint returned HTTP 200 in a prior fixture check. |
| Exactly 4 stars | Feedback saved screen first. Buy again opens only on explicit tap when offered. | 5-star navigation verified on Android. Fixture 202 returned BUY_AGAIN for exactly 4 stars. |
| 1–3 stars | Product-specific issues, optional note and photo, saved confirmation. An error leaves the draft in place. | Three-star screen and submission verified on earlier builds. Retry appearance reviewed in code. |
| One-time reorder | Suggested or custom delivery date, add to cart, explicit checkout, order success, then Subs → Orders. | Reorder sheet and live checkout verified separately on Android. |
| Address selection | Checkout sends the chosen address ID. Orders show the stored delivery address. | Live API verified order address ID. Android checkout passed with a complete address. |
| Subscriptions | Create, change, pause, resume, cancel. Scheduled and past plans stay in Subs. | Full lifecycle verified directly through the v5 API. Android creation and pause exercised. |
| Cart and recommendations | Server counts, quantity/date controls, clear, eligible reorder and pairing suggestions. | v5 endpoints passed. Android quantity PATCH and checkout passed. |
| Quality dashboard | Filter and aggregate feedback by product, issue and area. | Outside the Android contract. Requires dashboard access and separate verification. |

## Open differences

- The app uses Android's date picker for custom dates. Figma shows an inline calendar.
- Edit rating has no update endpoint in v5. Issue details can be submitted separately against the saved feedback ID.
- Eligible products can start a subscription from product details or the optional reorder sheet. One-time reorder goes through the cart.
- One earlier live feedback submission returned 404 after its rating was accepted. A separate test saved successfully. See BACKEND_REQUIREMENTS.md for the persistence investigation.
