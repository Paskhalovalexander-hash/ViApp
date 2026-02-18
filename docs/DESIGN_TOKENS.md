# Design Tokens (VitanlyApp)

`DesignTokens` lives in:
- `/Users/dantexme/AndroidStudioProjects/VitanlyApp/app/src/main/java/com/example/vitanlyapp/ui/design/DesignTokens.kt`

It contains the “tunable knobs” for UI: sizes, spacing, corner radii, elevations, animation durations, and (some) legacy colors.

Important distinction:
- Theme colors/gradients are defined in `/Users/dantexme/AndroidStudioProjects/VitanlyApp/app/src/main/java/com/example/vitanlyapp/ui/design/AppColorScheme.kt` and are accessed via `LocalAppColorScheme`.
- `DesignTokens` is mostly about geometry + consistent constants (and a few Classic defaults / legacy aliases).

---

## Where each UI object lives (and which tokens affect it)

### Tiles (Top/Middle/Bottom)

**Code:**
- `/Users/dantexme/AndroidStudioProjects/VitanlyApp/app/src/main/java/com/example/vitanlyapp/ui/component/Tile.kt`
- `/Users/dantexme/AndroidStudioProjects/VitanlyApp/app/src/main/java/com/example/vitanlyapp/ui/screen/main/MainScreen.kt`

**Key tokens:**
- Spacing/padding: `screenPadding`, `tileSpacing`, `tilePadding`
- Shape: `tileCornerRadius`
- Border: `tileBorderWidth` (gradient colors come from `AppColorScheme.borderGradientTop/borderGradientBottom`)
- Elevation: `tileElevationNormal`, `tileElevationExpanded`
- Expand/collapse motion: `tileTransitionDurationMs`, `tileScaleCollapsed`, `tileScaleExpanded`, `tileAlphaCollapsed`
- Layout weights (idle/collapsed/expanded): `tileWeightExpanded`, `tileWeightCollapsed`, `tileWeightIdleTopMiddle`, `tileWeightIdleBottom`
- Bottom tile shape: `bottomTileCornerRadius`

---

### KBJU Bars (Protein/Fat/Carbs + Calories)

**Code (typical usage in UI):**
- main/top tile screen code in `/Users/dantexme/AndroidStudioProjects/VitanlyApp/app/src/main/java/com/example/vitanlyapp/ui/screen/main/MainScreen.kt` and related composables

**Key tokens:**
- Colors (Classic defaults / also used for text tinting): `barProteinStart/End`, `barFatStart/End`, `barCarbsStart/End`
- Brushes: `barProteinBrush`, `barFatBrush`, `barCarbsBrush`
- Layout: `barHeight`, `barSpacing`, `barCornerRadius`, `barBorderWidth`, `barTextPaddingStart`, `barTopPadding`
- Overflow: `barOverflow`, `barBackground`

---

### “Weight plank” (current weight control)

**Code (typical usage):**
- top tile composables in `/Users/dantexme/AndroidStudioProjects/VitanlyApp/app/src/main/java/com/example/vitanlyapp/ui/screen/main/MainScreen.kt`
- wheel picker in `/Users/dantexme/AndroidStudioProjects/VitanlyApp/app/src/main/java/com/example/vitanlyapp/ui/component/WeightWheelPicker.kt`

**Key tokens:**
- Geometry: `plankHeight`, `plankCornerRadius`, `plankPadding`
- Wheel picker sizing: `plankWheelPickerHeight`, `plankWheelPickerWidth`
- Background: `plankBackground`
- Typography: `plankFontSizeLabel`, `plankFontSizeNumber`

---

### Chat (bottom tile content + input)

**Code:**
- `/Users/dantexme/AndroidStudioProjects/VitanlyApp/app/src/main/java/com/example/vitanlyapp/ui/screen/main/BottomTileContent.kt`

**Key tokens:**
- Bubble backgrounds: `chatBubbleUserBackground`, `chatBubbleAssistantBackground`
- Input: `chatInputBlockHeight`, `chatInputBlockCornerRadius`, `chatInputBlockBackground`
- Typography: `fontFamilyPlank`, `fontSizeChat`

---

### Food list (middle tile) — collapsed food cards

**Code:**
- `/Users/dantexme/AndroidStudioProjects/VitanlyApp/app/src/main/java/com/example/vitanlyapp/ui/screen/main/InputTileContent.kt` (`FoodEntryCard`, `DayPageContent`)

**Key tokens (section `FOOD ENTRY CARDS`):**
- Height: `foodEntryCardHeightCollapsed`
- Content padding: `foodEntryCardInnerPadding`
- Text row spacing: `foodEntryCardTextRowsSpacing`
- Background emoji: `foodEntryCardBackgroundEmojiSizeSp`, `foodEntryCardBackgroundEmojiAlpha`, `foodEntryCardBackgroundEmojiTintColor`, `foodEntryCardBackgroundEmojiTintAlpha`, `foodEntryCardBackgroundEmojiOffsetX/Y`, `foodEntryCardBackgroundEmojiNudgeY`, `foodEntryCardBackgroundEmojiRotationDeg`
- Glass: `foodEntryCardGlassBlurRadius`, `foodEntryCardGlassTintAlphaOverride`, `foodEntryCardGlassDebugAlpha`, `glassNoise`

**Spacing between cards:**
- In `DayPageContent` → `LazyColumn(verticalArrangement = Arrangement.spacedBy(...))` (not a token by default unless you add one).

---

### Expanded food card (modal overlay)

**Code:**
- `/Users/dantexme/AndroidStudioProjects/VitanlyApp/app/src/main/java/com/example/vitanlyapp/ui/screen/main/MainScreen.kt` (`ExpandingCardOverlay`)

**Backdrop behind modal (fullscreen):**
- Blur: `expandedOverlayBlurRadius`
- Darkening: `expandedOverlayTintAlpha`
- Vignette: `expandedOverlayVignetteAlpha`
- Noise: `glassNoise`

**Modal card appearance (section `Expanded food card (modal)`):**
- Corner radius: `expandedFoodCardCornerRadius`
- Width fraction: `expandedFoodCardWidthFraction`
- “Silhouettes glass” inside card: `expandedFoodCardGlassBlurRadius`, `expandedFoodCardGlassTintAlpha`, `expandedFoodCardGlassNoise`
- Base panel opacity under glass: `expandedFoodCardBaseAlpha`
- Border opacity: `expandedFoodCardBorderAlpha`
- Optional internal vignette: `expandedFoodCardVignetteAlpha`

**Actions row (bottom icons):**
- Icon sizing: `foodEntryCardActionIconSize`, `foodEntryCardActionRowHeight`

---

## Glassmorphism (global knobs)

**Tokens (section `GLASSMORPHISM`):**
- Blur: `blurRadius`, `blurRadiusLight`, `blurRadiusStrong`
- Tint alpha: `glassTintAlpha`
- Noise: `glassNoise`

**Where used:**
- Tiles blur (optional) in `/Users/dantexme/AndroidStudioProjects/VitanlyApp/app/src/main/java/com/example/vitanlyapp/ui/component/Tile.kt`
- Middle tile day indicator glass in `/Users/dantexme/AndroidStudioProjects/VitanlyApp/app/src/main/java/com/example/vitanlyapp/ui/screen/main/InputTileContent.kt`
- Food cards / expanded overlay as implemented in MainScreen/InputTileContent.

---

## Editing guidelines

- Prefer adding *new tokens* rather than hardcoding values in UI files when you expect iteration.
- Keep “expanded modal” tokens separate from “collapsed list card” tokens (they are tuned differently).
- If a color must follow theme, prefer `LocalAppColorScheme` over hardcoded `DesignTokens` colors.

