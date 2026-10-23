# Backend work requested by SSP1-86 (FE-3 Filter & Sort)

## Why

FE-3 ships category, price range, and sort controls. A condition filter was cut because `GET /api/listings` cannot filter by condition. The frontend placeholder is in `frontend/src/components/FilterBar.tsx`.

## Request

- Add an optional `condition` query parameter to `GET /api/listings`.
- Accept `NEW`, `LIKE_NEW`, `GOOD`, `FAIR`, and `WORN`, case-insensitively like `category`. Reject unknown values with `400 { "message": "..." }`.
- Combine condition with the other filters using AND.

## Where it plugs in

- `ListingBrowseController`: add `@RequestParam(required = false) String condition`.
- `ListingBrowseRequest.parse`: add `parseCondition` like `parseCategory`.
- `ListingSpecifications.visibleBrowseResults`: add one `equal(root.get("condition"), …)` predicate.
- Cover this in `ListingBrowseControllerTest` and `ListingBrowseQueryTest`.

## Response

Unchanged.

## Frontend follow-up once shipped

- Add `condition` to `browseParams.ts`, with lowercase URL values such as `like_new`.
- Add a Condition select in `FilterBar` at the placeholder comment.
- Send condition from `toSearchParams`.
