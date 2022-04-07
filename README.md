# Odysee Android

<a href="https://github.com/OdyseeTeam/odysee-android/blob/master/LICENSE" title="MIT licensed">
   <img alt="license" src="https://img.shields.io/github/license/OdyseeTeam/odysee-android?style=for-the-badge">
</a>


## Build from Source
To build the app, even as a debug APK, you will need to copy `app/twitter.properties.sample` file as `app/twitter.properties`

## Release
To create an APK file which can be installed on real devices, you will need to create a digital signature and then edit `app/build.gradle` file the following way:

```groovy
android {
    signingConfigs {
        release {
            storeFile file('<<put full path to the .JKS Java keychain file>>')
            storePassword '<<password of the file>>'
            keyAlias '<<the alias you chose for the digital signature>>'
            keyPassword '<<the password for the key>>'
        }
    }
(...)

    buildTypes {
      release {
          (...)
          debuggable false
          signingConfig signingConfigs.release
      }
```

Then you will be able to build a signed APK file via Build/Generate Signed Bundle/APK... menu item on Android Studio

## Contributing
We :heart: contributions from everyone and contributions to this project are encouraged, and compensated. We welcome [bug reports](https://github.com/OdyseeTeam/odysee-android/issues/), [bug fixes](https://github.com/OdyseeTeam/odysee-android/pulls) and feedback is always appreciated.

## [![contributions welcome](https://img.shields.io/github/issues/OdyseeTeam/odysee-android?style=for-the-badge&color=informational)](https://github.com/OdyseeTeam/odysee-android/issues) [![GitHub contributors](https://img.shields.io/github/contributors/OdyseeTeam/odysee-android?style=for-the-badge)](https://gitHub.com/OdyseeTeam/odysee-android/graphs/contributors/)

## License
This project is MIT licensed. For the full license, see [LICENSE](LICENSE).

## Security
We take security seriously. Please contact security@odysee.com regarding any security issues.

## Contact
The primary contact for this project is [@akinwale](https://github.com/akinwale) (akinwale.ariwodola@odysee.com)
