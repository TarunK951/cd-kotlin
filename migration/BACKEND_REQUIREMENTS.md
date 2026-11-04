# Backend requirements for sign-in and feedback

## Signup area selection

Registration currently requires phone, OTP, name, area and city. Android uses a small local list of area and city pairs. The signup journey does not collect a street address, flat number or PIN code.

Provide a public endpoint such as GET /api/service-areas returning stable area IDs, display names, cities and availability. Decide whether registration should accept an area ID or continue to accept area and city strings. Define a response for unserviceable areas so Android can explain the next step. The local choices do not imply serviceability.

## Reference behaviors requiring API support

- Edit a saved rating. The current contract saves on star tap and rejects a second rating. Define an authenticated update operation and which rating edits reset issue details.
- Provide the product code, order-specific delivery locality and usable product image URL on feedback prompts if these must appear in the delivery summary.
- Provide a way to resume incomplete issue details after a rating is saved and the form is dismissed.

## Quality dashboard and live feedback persistence

- The Android API guide has no quality dashboard or reporting endpoint. The client can verify feedback storage, but dashboard ingestion, order linkage and trend reporting require a backend contract and a dashboard test account.
- During Android QA, one three-star response opened the issue form but submitting returned “Feedback 356 not found.” The authenticated GET for that ID also returned 404. A separate direct test on account 101 returned feedback 355 and accepted its issue details with HTTP 200. Check that rating responses and subsequent issue saves resolve to the same persisted feedback and customer across deploys or instances.
