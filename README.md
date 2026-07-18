#  Radio Rave Android Native app

Radio website: https://radiorave.org

![Screenshot](./screenshot_phone.jpg "Screenshot")

This is a simple app using a background service to play an audio stream and a de-coupled UX interacting with the service. Metadata about the track being played is polled from a web API every 30 seconds.

Current requirements are: minimum OS: Android 6. Besides tablets and phones, it has been tested and should also work fine on Amazon Kindle, Amazon Fire TV, Android TV, Google TV (including the new Chromecast).

The released package is signed with radiorave.keystore (included in this repo) which is a development key with a public password (radiorave) — for update continuity when sideloading, NOT for authenticity. For a store, generate a private key (or use Play App Signing).



References about the architecture/patterns followed:
- https://developer.android.com/media/media3/session/background-playback
- https://medium.com/@janand1991/background-audio-playback-in-android-using-mediasessionservice-jetpack-compose-88214b02266d
- https://medium.com/@debz_exe/implementation-of-media-3-mastering-background-playback-with-mediasessionservice-and-5e130272c39e

