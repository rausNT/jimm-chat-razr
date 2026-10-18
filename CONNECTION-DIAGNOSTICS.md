# Local connection investigation — 2026-10-18

Base: 0.1.9, after withdrawing 0.2.0 and 0.2.1. This branch is diagnostic, not a demonstrated fix.

## Evidence

- ConnectAction maps 12% to STATE_INIT_DONE: socket initialization returned, waiting for SRV_CLI_HELLO.
- Original Icq.connect opens SplashCanvas with canCancel=false. Original ConnectAction.onEvent has no ON_CANCEL handler. Merely enabling the cancel command would not implement cancellation.
- Original disconnect returns when connected is false, including during login. Proper cancellation requires a separate transport lifecycle change and a blocking-I/O test.
- The 0.2 recovery code could cancel the splash timer while hiding a transient error, leaving a stale foreground progress screen.
- Existing ChatTest uses a stub Icq. Passing it is not evidence of network recovery on hardware.

## Diagnostic-only changes

0.1.11 retains 0.1.9 protocol and connection decisions. Fixed numeric events record start, stage, critical exception code (code * 100 + extension), socket open, EOF and reset. The ring stores 20 entries in memory. Credentials, payloads and addresses are not recorded. Connection screen shows progress, elapsed seconds and RX packet count; Help -> Diagnostics displays the event snapshot.

The JAR requests `javax.microedition.io.Connector.socket` in both its manifest and JAD. MIDP provides no portable API to read the Motorola permission decision in advance; the real socket open is the check and may trigger the system prompt. A `SecurityException` from that operation now becomes error 173 and diagnostic event 7. The app does not probe the network automatically when opened, because that would invoke the same permission prompt and create a network attempt without an explicit user action.

## Phone check

Install via local diagnostic EXE, retaining application data if offered. If the phone refuses downgrade from 0.2.1, do not delete data to force it. Record the displayed stage/time/RX at first stop and about 30 seconds later; after returning to the app, read Help -> Diagnostics. No automatic message replay or server probe is introduced.

## Follow-up

Use phone evidence to distinguish transport wait, packet parsing and stale UI. Then implement cancellation with real blocking transport tests, including cancellation before socket open returns, peer EOF, and stale worker cleanup. Publish only after describing what the phone actually verified.
