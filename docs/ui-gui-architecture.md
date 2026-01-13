# UI/GUI architecture decision

## Decision
We choose **Variant A**: the GUI framework (UI runtime) lives in **RealiteUI**.

## Rationale
- Keeps RealiteCore free of UI/runtime dependencies.
- Encourages feature plugins to publish data/actions only (Adventure-only), while UI rendering is centralized.
- Prevents duplicate GUI toolkits and avoids dependency cycles.

## Dependency rules
- **RealiteCore**: no GUI/toolkit abstractions, no UI runtime, no screen implementations.
- **RealiteUI**: owns the GUI framework/runtime and all concrete screens.
- **Feature plugins**: expose data/actions/events only; **no GUI**. Screens move to RealiteUI.
- **No second GUI toolkit**: all GUI logic must go through RealiteUI’s framework.
- **Adventure only**: any UI rendering must use Adventure APIs via RealiteUI.

## Migration guidance
- When a feature currently renders UI, move the screen/menus to RealiteUI.
- Plugins should instead publish the data and actions required for those screens.
