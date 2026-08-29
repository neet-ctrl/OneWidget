---
name: Android widget coordinate system
description: How the Sakura widget keeps dynamic content aligned with its artwork
---

For this widget, dynamic text must be rendered into the same bitmap coordinate system as the artwork and scaled with the actual AppWidget bounds. Fixed dp overlays drift when a launcher supplies a different widget height or adds host padding. The outside of the source artwork must remain transparent, and minute refreshes must be scheduled independently at minute boundaries.

**Why:** The launcher screenshot showed the XML overlays pinned near the top even though the browser mockup was correct; the root cause was fixed dp positioning being measured against different host bounds.

**How to apply:** Keep artwork and dynamic clock/date/weather content together in the provider’s proportional render path; do not reintroduce independent fixed-position TextViews for these elements. Invisible interaction hotspots must also use proportional weighted regions, not fixed dp coordinates. Treat transparency and exact refresh scheduling as separate native-widget concerns.