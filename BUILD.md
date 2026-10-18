# Build and source distribution — Jimm Chat 0.1.11

License for this modified work: GPL-2.0-or-later; see COPYING and NOTICE.txt.

## Contents

The source archive contains all application Java sources, readable language
data, original image/audio resources, compile-only Motorola API declarations,
and the scripts that control compilation and packaging. No other Jimm binary
or original workspace is needed. Existing per-file upstream notices remain.

## Requirements and build

1. Install Python 3.10+ and a JDK 17 (tested with OpenJDK 17.0.20).
2. Set JAVA_HOME to that JDK directory, or put its bin directory on PATH.
3. Extract the source ZIP to a new folder. Run `python build.py` there.
4. Find `dist/JimmChat.jar` and `dist/JimmChat.jad`.

On first build, build.py downloads four pinned build dependencies from Maven
Central; dependencies.json records their URLs and SHA-256 hashes. Alternatively
put those exact JAR files in `.deps/` or point JIMM_TOOLS to a folder containing
them, and run `python build.py --offline`. No software is installed system-wide.
These external compiler/platform libraries are not copied into the phone JAR.
The minimal Motorola declarations in api-stubs/ compile separately and are also
excluded from the phone JAR. The actual API implementation comes from the phone.

src/ is the preferred editable Java source after upstream conditional
preprocessing; do not run the old integrate/preprocess scripts over it.
src/lng/RU.lang is the readable localization source, compiled by package.py.
resources/ contains original unchanged PNG/audio assets. See RESOURCES.md.
The build recreates only its own disposable `.build/` directory.

## Checks

Run `python test.py` after building. Tests use desktop MIDP/RMS/network adapters;
they do not connect to the service or send real messages. They do not replace a
device installation test. Deterministic ZIP timestamps permit comparing rebuilt
JAR hashes with the bundle's CHECKSUMS.sha256 when using the tested toolchain.

## Installation

Transfer JimmChat.jar to a compatible MIDP 2.0 / CLDC 1.0 phone using its normal
Bluetooth file-receive/Java installation workflow. Accept installation. When
updating the same suite, choose to retain its saved data if asked. No flashing,
P2K modification, USB driver changes or firmware updates are needed.

## Variants

VARIANT.txt identifies the variant. The PRIVATE bundle includes the owner's
embedded login and exact corresponding source. Keep it private; GPL does not
require publishing your private copy on the internet.

The PUBLIC bundle has empty UIN/password defaults. The server remains
icq.jimm.online:5190, TCP/Socket, no proxy, with the other common defaults kept.
Autoconnect is initially off. First launch without a UIN opens My account with editable UIN, masked password,
server and port. Save your settings, then connect from Application. This initial account setup
does not contain or retrieve the original owner's credentials.

Public and private binaries differ: each bundle contains its own matching
source ZIP. Do not substitute sanitized source for a personalized binary.

## Passing the program on

Distribute the whole PUBLIC release bundle, including its matching source ZIP,
COPYING, NOTICE.txt and CHANGES.md. Recipients can rebuild, modify and redistribute
under GPL-2.0-or-later. This is the accompanying-source method in GPLv2 section
3(a), not a promise to supply sources later. No warranty is provided.

## Windows companion

See windows/README.md for the EXE build, synthetic self-tests, and the optional personal.dat format.
