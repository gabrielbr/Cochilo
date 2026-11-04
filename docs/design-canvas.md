# The design canvas (screens and flows, for annotation)

**https://claude.ai/artifact/5gWMnzwV7mcXAedmQ4kHa1**: a claude.ai Design artifact that holds every
screen of the app and its main flows, so the owner can comment on any of them. It's private to the
owner until shared from its Share menu.

## What's on it

- **Flows** page:
  - **App map** (the entry): the four tabs, plus Ask Cochilo and Settings from the header.
  - **One artboard per flow:** a nap, the night, logging, the day and editing, Ask Cochilo, pairing
    a phone, first run, and updates. Each is the real screens in order, joined by arrows labelled
    with the tap that moves you on.
- **Screens** page: one artboard per area, every screen of that area side by side, at phone size:
  - Today and Day;
  - Patterns and From Tatá;
  - logging and sheets, and Ask Cochilo;
  - Settings and pairing;
  - first run and outside the app (notification, widget, report, celebration);
  - tablet and foldable.

Every screen is the app's own Paparazzi screenshot, so the canvas shows exactly what the app
renders.

## The rule (AGENTS.md)

Every change that adds or changes a screen or a flow updates this canvas in the same PR:
1. **Upload** each changed or new screenshot to the artifact as an asset (the Artifact tool:
   `publish`, `url` above, `asset: true`, `file_paths` up to 25 per call).
2. **Map it** in `design/canvas/build_canvas.py`:
   - put the returned `/_blob/…` url in `B`;
   - for a new screen, add its entry to `B` and `SNAP` and place it in a `SECTIONS` row;
   - for a new or changed flow, update `FLOWS`: the screens in order and the tap between each.
3. **Build:** `python3 design/canvas/build_canvas.py <scratch folder>`.
4. **Publish** the changed artboards to the same `url` with `root` = that folder. Send
   `project/canvas.json` too only when artboards were added, removed or resized. Before you
   send it, read the live one and keep its `createdOnFiles` and anything the owner added (notes,
   arrows, comments live with the canvas).
5. **Reply on every comment** the owner left on a screen the change touched. Resolve it only once
   the change ships.

The owner's annotations are comments on the canvas: read them (`ArtifactComments`) before changing
a screen they're on.
