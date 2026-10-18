# Jimm Chat for Motorola RAZR

**Twenty years of messaging UX lessons, brought back to a Java feature phone.**

> **Experimental. It may not work on your phone.** Hardware testing has only used our Motorola RAZR V3i (identified by tools as V3ire). No other handset is verified. New automatic recovery is covered by desktop tests, but real connection-loss recovery on this phone has not yet been verified.

An independent, experimental fork of **Jimm 0.5.1** for [Jimm.online](https://jimm.online/), focused on Motorola RAZR V3i/V3ire: conversations first, fewer menu steps, and faster typing on a physical keypad.

[Русская ветка](https://github.com/rausNT/jimm-chat-razr/tree/russian) · [Build instructions](BUILD.md) · [Changes](CHANGES.md) · [License](COPYING)

This is not an official Jimm.online or Motorola release, and it is not a Telegram client. It uses the Jimm protocol implementation with a redesigned chat interface. The application UI is currently **Russian**; the branch names describe the documentation language.

## What changes

- A conversation list with message previews, unread counters and saved drafts.
- Compact message bubbles and a single-line header for a small screen.
- Three top-level sections: My account, New chat, Application; account comes first.
- Three chat actions: Write, Quick reply, Smileys.
- Nine editable quick replies, saved locally between launches.
- Keys **1–9** choose a reply or text smiley inside its picker.
- Replies and smileys insert at the editor caret, preserving surrounding text. Sending is a separate action.
- Up to 40 locally stored messages per chat, with an approximately 96 KiB archive budget.

## Automatic network recovery

Transient connection failures retry after 5, 15, 30 and then 60 seconds. The old connection worker must finish before another starts. Manual Disconnect, exit, invalid credentials, invalid server settings and rate-limit errors stop retries. Messages are never automatically replayed.

After two minutes without received packets, a read-only OSCAR ICBM parameter query checks server responsiveness; 45 seconds without incoming traffic triggers recovery. Connection attempts have a 90-second watchdog; platform blocking socket operations can delay cleanup. Compatibility of the probe with the deployed server still needs real-phone testing. The chat-list header shows retry status; Application → Disconnect stops it.

## Designed for buttons

| Where | Key/action | Result |
| --- | --- | --- |
| Conversation | Left softkey | Write a message |
| Conversation | Center / 5 / Menu | Open the three chat actions |
| Conversation | 1 / 3 | Quick replies / smileys |
| Reply or smiley picker | 1–9 | Insert the matching item into the draft |
| Reply picker | Select with arrows, then Edit | Replace that saved phrase |
| Message editor | Quick reply / Smileys command | Insert at the current caret |

The design goal is fewer presses and predictable navigation. This is an ongoing experiment, not a claim that every modern messenger feature fits a 2000s phone. Smileys are currently text; animated sticker packs and predictive typing are not implemented.

## Device and release status

Target: Motorola RAZR V3i/V3ire, MIDP 2.0 / CLDC 1.0. Hands-on development used a **V3ire**; compatibility with every V3i variant is not established.

Earlier personalized builds connected to Jimm.online on the physical phone. Version **0.2.0** passed desktop adapter tests including editable account validation and saving; the new account form still needs confirmation on-device. The public build is compiled and tested separately and contains no embedded account.

## Windows setup companion

The portable Windows companion builds a personalized JAR in memory and sends it to an explicitly selected paired Bluetooth phone. Server `icq.jimm.online` and port `5190` are prefilled and editable. UIN and password are entered by the user. No Java/Python installation is needed to run the EXE. See [Windows setup](windows/README.md). ZIP/configuration and form tests pass; end-to-end transfer from this new EXE still needs hardware verification.

## Build and try

Install **JDK 17** and **Python 3.10+**, then run:

```sh
python build.py
python test.py
```

The first build downloads four pinned, hash-verified build dependencies. Output: `dist/JimmChat.jar` and `dist/JimmChat.jad`. See [BUILD.md](BUILD.md) for offline builds and dependency provenance.

Create your own account at [Jimm.online](https://jimm.online/). On first launch, enter your UIN and password in My account (UIN, masked password, server and port), save, and connect. Defaults retain `icq.jimm.online:5190`, TCP/socket, no proxy; autoconnect starts disabled. Your mobile network Java/GPRS profile must already work.

Transfer the JAR using the phone's normal Bluetooth/Java installation flow. No firmware patch is required. When upgrading the same suite, retain saved data if prompted. The existing suite vendor/name are preserved for data continuity.

## Development

The main UI lives in `src/jimm/SimpleChat.java`; the existing Jimm code supplies networking and protocol handling. Sources are preprocessed, editable Java; resources and language text are included. Tests use desktop adapters for MIDP, RMS and transport, with no real messages sent. They cover drafts, menu navigation, numeric choices, caret insertion and persistent custom replies; they do not replace hardware testing.

Contributions should keep the main menus short, preserve drafts, avoid accidental sends, and include a small-screen/keypad test scenario. Please report the exact device variant, app version and reproduction steps. Never attach passwords or private chat history.

## License and credits

**GPL-2.0-or-later.** Original Jimm: Copyright (C) 2003–06 Jimm Project. Original per-file notices remain. This independently modified version retains the upstream protocol implementation and resources; see [NOTICE.txt](NOTICE.txt), [RESOURCES.md](RESOURCES.md) and [CHANGES.md](CHANGES.md).

When distributing binaries, accompany them with their matching source and build instructions. No warranty is provided.
