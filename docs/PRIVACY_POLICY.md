# Privacy Policy for Startune Music

Last Updated: November 2026

## Introduction

Startune Music is an independent Android music player application designed for music streaming, local audio playback, and media sharing features. This Privacy Policy explains how Startune Music handles user information, permissions, and third-party integrations.

By using Startune Music, you agree to the practices described in this Privacy Policy.

---

## Information We Collect

Startune Music is designed with a privacy-first approach. We do not sell personal data, profile users for advertisement networks, or collect unnecessary personal information.

### Information Collected & Processed Locally

The application may locally process:

* **Device & Hardware Information:** Audio format compatibility, display metrics, and audio hardware capabilities.
* **Audio Metadata:** Track titles, artist names, album art, durations, and folder structures for local music organization.
* **Cache Data:** Temporary storage for audio buffers, lyrics, and Deezer artwork thumbnails to facilitate fast, offline playback.
* **Diagnostics & Crash Logs:** Anonymized logs generated locally to assist in troubleshooting playback stability.

All local library data is processed directly on your device.

---

## Device Permissions & Usage

Startune Music requests only the permissions necessary to deliver core playback and media management functionality:

* **Music and Audio / Storage:** To discover, index, and play local audio files and display embedded album art.
* **Internet Access:** To stream audio from user-authorized sources, query LRCLIB for synchronized lyrics, and fetch high-resolution artwork.
* **Foreground Service & Media Playback:** To ensure uninterrupted background playback and seamless notification/lockscreen controls.
* **Bluetooth / Connectivity:** To route audio output to wireless headphones and Bluetooth speakers.

Startune Music does not upload your local media files or library indexes to any external servers without explicit user initiation.

---

## Third-Party Services & Integrations

Startune Music provides optional integrations with third-party platforms to enhance your music experience:

### 1. Self-Hosted Libraries (Navidrome/Subsonic, Jellyfin)
* You log in with your own server. Credentials are stored on-device and used solely to sync your library, stream, and download music for offline listening.

### 2. Lyrics, Metadata and Scrobbling (LRCLIB, Deezer, MusicBrainz, Last.fm, ListenBrainz)
* Public APIs are queried solely for lyrics fetching, high-resolution artist artwork retrieval, and on-demand metadata matching.
* Scrobbling listening history to Last.fm and ListenBrainz is opt-in: nothing is submitted until you connect your account, and disconnecting deletes queued items.

Song and lyric cards are shared through the Android share sheet. Cards are composed locally on-device.

Third-party services are subject to their respective terms of service and privacy policies. Startune Music is not affiliated with any third-party service provider.

---

## Data Storage & Retention

Startune Music stores all user configuration, playback history, cached lyrics, and playlists locally on-device via Room SQLite databases and encrypted preferences.

* Users can clear cached lyrics, images, or reset app databases at any time in the app settings.
* Uninstalling the application completely removes all locally stored app data.

---

## Data Sharing & Third-Party Disclosure

Startune Music does **not** sell, rent, or trade user data. We do not integrate third-party ad networks, trackers, or commercial telemetry SDKs.

Information is transmitted externally only when:
* Directly requested by you (e.g., streaming a track, querying lyrics, sharing a song card).
* Required to communicate with user-configured third-party service APIs.

---

## Security

Startune Music incorporates modern security standards to protect your local data, including scoped storage compliance and secure sandbox boundaries. However, users are encouraged to maintain proper device-level security (PINs, biometric locks) to protect on-device data.

---

## Children's Privacy

Startune Music does not knowingly collect or solicit personal information from children under the age of 13. If you believe any unauthorized data has been collected, please contact us.

---

## Open Source & Transparency

Startune Music is developed transparently for the community. Documentation, security policies, and legal notices are accessible across official project channels.

---

## Changes to This Policy

We may occasionally update this Privacy Policy to reflect app updates and regulatory requirements. Revisions will be published with an updated "Last Updated" date.

---

## Contact

For questions, feedback, or privacy inquiries regarding Startune Music:

* **GitHub Repository:** [https://github.com/abdrhmniqbal/startune-music](https://github.com/abdrhmniqbal/startune-music)
