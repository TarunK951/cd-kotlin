# Backend requirements for sign-in and feedback

## Signup area selection

Registration currently requires phone, OTP, name, area and city. Android uses a small local list of area and city pairs. The signup journey does not collect a street address, flat number or PIN code.

Provide a public endpoint such as GET /api/service-areas returning stable area IDs, display names, cities and availability. Decide whether registration should accept an area ID or continue to accept area and city strings. Define a response for unserviceable areas so Android can explain the next step. The local choices do not imply serviceability.

## Reference behaviors requiring API support

- Edit a saved rating. The current contract saves on star tap and rejects a second rating. Define an authenticated update operation and which rating edits reset issue details.
- Reorder as a subscription. The connected cart places one-time orders. Provide a subscription endpoint or purchase mode with schedule, pricing and eligibility rules.
- Provide the product code, order-specific delivery locality and usable product image URL on feedback prompts if these must appear in the delivery summary.
- Provide a way to resume incomplete issue details after a rating is saved and the form is dismissed.
