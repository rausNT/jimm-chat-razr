# 0.1.8 — 2026-10-18

- Editable UIN, masked password, server and port directly in My account for both variants. Existing saved settings override embedded defaults. Validate before save; persist active account slot; disconnect before changes; restore memory values on save failure. Back cancels. Public first launch uses the same form.

# 0.1.7 — 2026-10-18

- Expose Write, Quick reply and Smileys directly in the native Motorola command menu. Remove the nested Menu command in conversations; retain direct left Write and right Back. Regression checks cover direct dispatch and cleanup on return home.

# Changes from Jimm 0.5.1

License: GPL-2.0-or-later. This is an independent modified version.

## 2026-10-17

- All upstream Java sources were preprocessed using the upstream SiJaPP tool for MOTOROLA with HISTORY, FILES, SMILES, TRAFFIC, PROXY and DEBUGLOG disabled. Per-file notices identify this mechanical change separately from functional edits.
- Options.java: Jimm.online endpoint, automatic connection and personal defaults in the private build.
- Jimm.java / ContactList.java: main application and contact-list navigation routed to SimpleChat.
- ChatHistory.java: incoming/outgoing message hooks for the new UI.
- SimpleChat.java: new conversation list, bubbles, unread counts, drafts, local RMS archive (40 messages per chat / 96 KiB total), existing-roster import, three-section menu and single-line header.

## 2026-10-18

- SimpleChat.java: quick phrases and text smile selector; keys 1 and 3; explicit send remains required.
- License packaging: COPYING and NOTICE.txt in the JAR and release bundles; full license accessible from help; dated notices in changed files.
- New portable build.py/package.py/build.ps1, dependency hashes and clean source bundles. Compiling no longer needs another Jimm JAR or the original workspace paths.
- RU language resources are now generated from the included readable src/lng/RU.lang with full keys, matching the unshortened source. Earlier packages reused upstream compacted keys.
- Public variant: account defaults removed, automatic login disabled. On first launch with no UIN, upstream account settings are available for initial configuration. Private variant retains the owner's existing defaults. These variants have separate corresponding-source bundles.

Source files in src/lng were mechanically preprocessed on 2026-10-17; resource PNG/audio files are copied unchanged from the upstream Motorola RU light release. No firmware, driver or phone configuration changes are part of this release.

## 2026-10-18 — 0.1.5

SimpleChat.java: central FIRE/5 and the chat menu command open exactly three actions (Write, Quick reply, Smileys). Left Write still opens the editor directly, Back returns to the chat. Phrases/smileys insert into the editor without sending.
# 0.1.6 — 2026-10-18

- SimpleChat.java: nine editable phrases in a separate RMS store; numeric 1–9 selection for phrases and smileys; insertion at the captured editor caret, preserving surrounding text. No automatic sending. Existing three-entry chat menu retained.
- tests/ChatTest.java and make-stubs.py: caret, numeric selection, phrase persistence, cancellation and independent record stores covered.
- Version, attribution and matching source bundles updated. Original copyright and GPL-2.0-or-later retained.

