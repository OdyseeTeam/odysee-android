# Odysee Android

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
Contributions to this project are welcome, encouraged, and compensated. For more details, see https://lbry.io/faq/contributing

## License
This project is MIT licensed. For the full license, see [LICENSE](LICENSE).

## Security
We take security seriously. Please contact security@lbry.com regarding any security issues. Our PGP key is [here](https://keybase.io/lbry/key.asc) if you need it.

## Contact
The primary contact for this project is [@akinwale](https://github.com/akinwale) (akinwale@lbry.com)
