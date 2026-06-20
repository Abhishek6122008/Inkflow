# Figma Design Prompt — Inkflow "Pixel Studio" UI

Paste the sections below into Figma's AI design tool (or any AI UI generator) one at a time, in order. Each section is a self-contained prompt. The goal is a retro pixel-art tool aesthetic — think **Aseprite** — applied to a collaborative rich-text editor instead of a sprite editor.

Inkflow currently has exactly 4 screens implemented: **Login, Register, Document List, Document Editor**. Design all 4. There is no real-time collaboration UI yet (no cursors/avatars) and no sharing/permissions UI yet (every document is single-owner) — don't design for those; they'll come in a later pass.

---

## 0. Global style prompt (paste first, establishes the design system)

```
Design a UI kit called "Pixel Studio" for a desktop web app, styled like the
Aseprite pixel-art editor's chrome — NOT like a sprite/pixel-art canvas itself,
just the surrounding tool UI.

Core visual language:
- Dark, low-saturation charcoal/gray background (#2b2b30 to #1e1e22 range),
  so the actual document content (black text on white/cream paper) is the
  brightest, highest-contrast element on screen and pops against the chrome.
- Sparse, deliberate accent color: one warm accent (amber/orange, like
  Aseprite's selection orange) used only for active states, focus rings,
  and primary actions. Everything else stays grayscale.
- Blocky, beveled "pixel button" style: 1-2px hard-edged borders, a visible
  light-on-top/dark-on-bottom bevel (like an old Windows 95 / retro game UI
  button), NO rounded corners anywhere, NO soft drop shadows or blurs.
- A small bitmap-style monospace or pixel font for UI labels, toolbar
  buttons, and panel titles (e.g. "Press Start 2P", "VT323", or a similar
  pixel webfont). Body text inside the actual document stays a normal
  readable serif/sans font — pixel styling is for chrome only, never for
  document content.
- Tiny (16x16 or 24x24) blocky icon style for toolbar buttons — flat,
  2-3 color icons, no gradients or anti-aliased curves.
- Panels are clearly "docked": flat rectangular regions with a 1px border
  separating them from the canvas/content area, like Aseprite's
  tools/layers/color panels docked around the canvas.

Components to produce in the kit:
- Primary button (beveled, accent-colored)
- Secondary/normal button (beveled, gray)
- Text input (flat dark field, hard border, pixel-font label above it)
- Icon-only toolbar button (24x24, toggled/active state shown with
  inset bevel + accent border)
- Tab chip (for the document tab strip — active vs inactive state)
- List row (for the document list — hover and selected states)
- Modal/dialog frame (beveled border, pixel-font title bar, like an
  Aseprite popup dialog)
- Small inline status pill (e.g. "Saved", "Saving...")

Keep all of this restrained — it should read as "a serious tool with a
retro skin," not a cartoon or game UI. Reference: Aseprite, Dwarf Fortress
modern UI, old-school DAW software (FL Studio, Cubase) chrome.
```

---

## 1. Login screen

```
Design the Login screen for "Inkflow", using the Pixel Studio kit above.

Layout: centered card, max-width ~360px, on the dark charcoal background.
Inside the card, top to bottom:
- "INKFLOW" wordmark in the pixel font, larger size, accent-colored
- Email input (label "EMAIL" above field, pixel font, all-caps)
- Password input (label "PASSWORD" above field, obscured text)
- Primary beveled button, full width of the card, label "LOG IN"
  (label changes to "LOGGING IN..." while submitting — show both states)
- Below the button, a plain text link in a dimmer gray: "Don't have an
  account? Register" — the word "Register" in the accent color
- An error state: a thin accent-bordered (use a red/orange warning
  variant of the accent, not the primary amber) banner above the button
  showing a sample error like "Invalid email or password"

Show 3 variants: default/empty state, loading state (button disabled +
"LOGGING IN..."), and error state (banner visible).
```

---

## 2. Register screen

```
Design the Register screen for "Inkflow", using the Pixel Studio kit.
Same card layout and position as the Login screen for visual consistency.

Fields top to bottom: "DISPLAY NAME", "EMAIL", "PASSWORD" — same input
style as Login. Primary button labeled "CREATE ACCOUNT" (loading state:
"CREATING ACCOUNT..."). Below the button, dimmer text: "Already have an
account? Log in" with "Log in" in accent color.

Same 3 variants as Login: default, loading, error (e.g. "Email already
in use").
```

---

## 3. Document List screen ("My Documents")

```
Design the Document List screen for "Inkflow", using the Pixel Studio kit.
This is the screen a user lands on right after logging in.

Top bar (full width, docked like an Aseprite top toolbar):
- Left: pixel-font page title "MY DOCUMENTS"
- Right: a secondary beveled button "LOG OUT"

Main content area (dark background): a vertical list of document rows.
Each row is a flat rectangular panel (the "List row" component) showing:
- Document title (pixel font, medium size)
- Small dimmer subtext: "Updated [relative time]" e.g. "Updated 2 hours ago"
- Right-aligned: two small icon-only toolbar buttons — a pencil/edit icon
  (rename) and a trash icon (delete) — both 24x24, only visible/highlighted
  on row hover
Rows have a hover state (subtle lighter background) and the whole row is
clickable to open the document.

Bottom-right corner: a floating circular accent-colored "+" button (like
a floating action button) to create a new document — but keep it blocky/
beveled rather than a soft material-style circle; an octagon or square
button with a "+" pixel icon is fine.

Empty state: when there are no documents, show centered dimmer text
"No documents yet — tap + to create one." with the + button still visible.

Also design two small modal dialogs in the Pixel Studio dialog style:
- Rename dialog: title "RENAME DOCUMENT", one text input pre-filled with
  the current title, "CANCEL" (secondary) and "SAVE" (primary) buttons
- Delete confirmation dialog: title "DELETE DOCUMENT", body text
  'Delete "[Document Title]"? This cannot be undone.', "CANCEL" (secondary)
  and "DELETE" buttons — make the DELETE button use a red/warning accent
  instead of the normal amber accent

Show: list with 4-5 sample documents, empty state, rename dialog open,
delete dialog open.
```

---

## 4. Document Editor screen (the main "canvas" screen)

```
Design the Document Editor screen for "Inkflow", using the Pixel Studio
kit. This is the most Aseprite-like screen — think of the document as
the "canvas" and the surrounding chrome as the docked tool panels.

Top bar (docked, full width):
- Left: a back/arrow icon button (returns to document list)
- Center-left: the document title in pixel font
- Right side: an inline status pill showing either "SAVING..." (dimmer,
  with a tiny spinner icon) or "SAVED" (calm gray, checkmark icon) —
  design both states
- Far right: secondary beveled "LOG OUT" button

Below the top bar, a docked horizontal toolbar strip (like Aseprite's
tool options bar) containing the rich-text formatting controls as
icon-only 24x24 toggle buttons in a row: bold, italic, underline,
strikethrough, ordered list, bullet list, blockquote, code block, link,
text color, undo, redo. Group them in small clusters separated by a thin
vertical divider line, matching how Aseprite clusters related tool icons.
Show 2-3 of these buttons in an active/toggled state (inset bevel +
accent border) to demonstrate the active state.

Main area: the document "canvas" — a bright white/cream paper-colored
rectangle centered in the dark workspace with generous padding around it
(like Aseprite's canvas floating in the dark gray workspace), containing
sample rich text content (a heading, a paragraph, a bullet list) in a
normal readable serif font — this is the one place pixel-font styling
should NOT be used, to keep document content legible and separate from
the tool chrome.

Optional but encouraged: design a docked tab strip ABOVE the top bar,
showing 2-3 open-document tabs (pixel font labels, active tab has accent
underline/border, inactive tabs dimmer) plus a small "+" tab at the end
to open another document from the list — this previews future
multi-document support even though only one document is open at a time
today.

Show: normal editing state, and the "SAVING..." vs "SAVED" status pill
states side by side.
```

---

## Notes for whoever runs these prompts

- Run section 0 first in its own Figma AI conversation/page so the style tokens (colors, button components, fonts) get created once and reused — then reference "use the Pixel Studio kit" in each screen prompt if the tool supports carrying context forward, or paste the kit's resulting colors/components manually into each new prompt if it doesn't.
- The tab strip in section 4 is aspirational (Phase 3+ feature, not yet built) — flag it to the designer/output as a "preview, not required for v1" so it doesn't block using the rest of the editor design today.
- Real-time collaborator cursors/avatars and sharing/permission UI (viewer vs editor badges, "Share" button, collaborator list) are deliberately excluded from all 4 prompts above — those map to Phase 3 (`docs/roadmap.md`) and Phase 2's deferred `DocumentCollaborator`/`DocumentRole` work, respectively. Add a fifth prompt for those once that backend work lands.
