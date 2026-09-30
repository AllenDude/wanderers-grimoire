# Wanderer's Grimoire

Native Kotlin Android app. Notes, pins (links), and tasks, organized into
folders you make yourself (Recipes, Useful things, whatever). Click a folder
to see everything filed under it. Everything is stored on-device with
SharedPreferences, no backend, no account.

## Getting the APK without installing Android Studio

Same setup as the classboard app:

1. Push this whole folder to a new GitHub repo.
2. GitHub Actions builds automatically on every push to `main` (see
   `.github/workflows/build-apk.yml`).
3. Go to the repo's **Actions** tab, open the latest run, scroll to
   **Artifacts**, and download `wanderers-grimoire-debug-apk`. It's a zip with
   `app-debug.apk` inside.
4. Move the APK to your phone and install it. You'll need to allow installs
   from that source once (Android will prompt you).

You can also trigger a build manually from the Actions tab without pushing,
using the "Run workflow" button (`workflow_dispatch`).

This builds a debug APK signed with the default debug key, fine for personal
use. Not set up for Play Store distribution.

## Updating without uninstalling first

Two things used to break this: every CI build signed the APK with a
throwaway debug key (fresh machine each time, so Android saw each build as
a different app and refused to update over the old install), and there was
no version bump, so even a matching signature could look like the same
version. Both fixed now:

- `keystore/debug.keystore` is a fixed debug key committed to the repo on
  purpose (debug keys aren't meant to be secret, and this one is never used
  for anything but sideloaded personal builds). Every CI build signs with
  it, so the signature always matches the last install.
- `versionCode` and `versionName` are passed in from the GitHub Actions run
  number (`app/build.gradle` reads `buildVersionCode` / `buildVersionName`),
  so every pushed build is automatically a higher version than the last.

With both matching, installing a new APK over the old one now updates in
place, no uninstall needed, and everything in local storage (your notes,
pins, tasks, folders) survives the update same as any normal app update.
This only breaks again if you uninstall manually, or if `keystore/debug.keystore`
gets deleted or regenerated.

## Project layout

```
app/src/main/java/com/allen/wanderersgrimoire/
  Models.kt              Folder and Item data classes (type is "note"/"link"/"task", plus a pinned flag)
  Storage.kt               reads/writes JSON to SharedPreferences, migrates old "pin" type to "link"
  ImageStore.kt              saves shared-in or picked images to internal storage, sampled thumbnail decoding
  FeedUtils.kt                 day-group labels (TODAY/YESTERDAY/date), time formatting, URL domain extraction
  FeedAdapter.kt              header row + text card + image tile, one adapter for both Home and Pinned
  CollectionAdapter.kt   collection list rows with item counts, used by the Collections screen
  MainActivity.kt       screen switching, search, sort, filters, quick capture, share-intent capture
app/src/main/res/              layouts, colors, styles, strings, launcher icons
.github/workflows/          the build pipeline
```

Share capture works through two intent filters on `MainActivity` in
`AndroidManifest.xml` (`text/plain` and `image/*`), handled in
`handleShareIntent()`. A shared image gets copied into
`filesDir/images/` immediately, since the `content://` Uri another app
hands over is only valid for the life of that share, not permanently. The
in-app Quick Capture "Image" button uses the same storage path, just fed
from the system image picker instead of a share intent.

Home and Pinned use a `GridLayoutManager` with a span-size lookup rather
than a plain vertical list: a day header or a text card (note/link/task)
takes the full row width, but anything with an attached image collapses to
half width, so images naturally fall into a 2-column grid alongside the
regular cards instead of each getting a full-width slot to itself.

## Screens

Five destinations via the bottom nav:

- **Home** — header with a small stat line (entries/collections/pinned),
  a search bar with a sort-direction toggle, type filter chips (All/Notes/
  Links/Tasks/Images/Pins), a "RECENT" section label that changes to match
  whatever filter is active (with a "See all" to clear it), then the feed
  itself, grouped into TODAY/YESTERDAY/date sections. An attached image
  renders as a compact grid tile (thumbnail, star, one-line caption); a
  link shows its domain beneath the title; a task gets a checkbox;
  everything else gets a star toggle, edit/delete, and a timestamp.
- **Collections** — every folder with a live item count, "All saved" at
  the top, delete per collection (items inside become unassigned, not
  deleted), and a button to add a new one. Tapping a collection jumps to
  Home filtered to it, with a banner to clear that filter.
- **➕ (floating, center)** — Quick Capture, a bottom sheet with four
  content types (Note/Link/Image/Task). Note/Link/Task open the same add
  dialog as before; Image opens the system image picker and pre-fills the
  dialog with the picked photo. The dialog also has a Delete button now
  when editing an existing item, not just the card's own Delete control.
- **Pins** — everything you've starred, across every type and collection,
  newest first, no day grouping, same grid treatment for images.
- **Settings** — app version, a short privacy note, and a shortcut into
  Collections for management.

## Animations

Kept deliberately understated, nothing longer than ~220ms, no bounce/
overshoot interpolators:

- Lists (Home, Pinned, Collections) cascade in with a quick fall+fade
  whenever the data actually changes, not just on first load, via a
  `LayoutAnimationController` replayed with `scheduleLayoutAnimation()`
  after every `notifyDataSetChanged()`.
- Switching bottom-nav screens crossfades instead of snapping.
- The section label ("RECENT" / "NOTES" / etc.) crossfades too, but only
  when the label actually changes, not on every keystroke while searching.
- The floating capture button has a real ripple and a press-in squish
  (scales down on finger-down, springs back on release), matching how a
  Material FAB actually behaves.
- Tapping a star gives it a quick pop. Same for the checkbox on a task.
- The empty state fades in the first time it appears, not every refresh.
- Every chip, nav item, card button, and row now has proper ripple touch
  feedback (`selectableItemBackground`), which it was missing entirely
  before, taps just fired with zero visual response.

One real bug got fixed in the process: marking a task done never actually
changed how the card looked before, just the checkbox. It's now properly
struck through and dimmed.

## What's in vs left out

In: notes, links with a domain-aware card, tasks with checkboxes, starring
any item (with its own filter chip and its own bottom-nav tab), custom
collections with counts and delete, search, sort direction toggle, a
day-grouped home feed with a 2-column image grid, content-aware cards,
Quick Capture, delete from the edit dialog, and capturing straight from
other apps or the system image picker.

Left out for now, same spirit as the classboard MVP: no drag-to-reorder, no
cloud sync or backup/export, no OCR (text inside a saved screenshot isn't
searchable yet), no way to remove or swap an image after attaching it, no
multi-select/bulk move, no collection rename (delete and recreate works for
now), no per-collection icons (all use 📁), search only covers the Home
feed, not Pins or Collections. All addable later without touching the data
model.

## Editing colors

`app/src/main/res/values/colors.xml`. A night palette (deep indigo paper,
gold and parchment-cream ink) pulled from the app icon artwork, plus a
distinct hue per content type: `note` (moonlit blue), `link` (violet),
`task` (teal), and `gold` doing double duty as the pinned-star color and
the theme's primary accent (chips, the floating capture button, active nav
tab). There's no separate light theme right now, it's the same dark
palette regardless of system day/night setting.

## App icon

Lives in `app/src/main/res/mipmap-*/`, one PNG per screen density
(`ic_launcher.png` and `ic_launcher_round.png`). To swap it, replace all five
density versions with your new image resized to 48/72/96/144/192px.
