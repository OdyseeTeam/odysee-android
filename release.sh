#!/bin/bash
./gradlew assembleRelease --console=plain
version=$(./gradlew -q printVersionName --console=plain | tail -1)
mkdir -p bin/
rm -f bin/*
cp app/build/outputs/apk/release/app-release-unsigned.apk bin/odysee-$version-release-unsigned.apk

# sign APK
echo "Signing APK..."
jarsigner -verbose -sigalg SHA1withRSA \
    -digestalg SHA1 \
    -keystore lbry-android.keystore \
    -storepass $KEYSTORE_PASSWORD \
    bin/odysee-$version-release-unsigned.apk lbry-android > /dev/null \
    && mv bin/odysee-$version-release-unsigned.apk bin/odysee-$version-release-signed.apk
zipalign -v 4 \
    bin/odysee-$version-release-signed.apk bin/odysee-$version-release.apk > /dev/null \
    && rm bin/odysee-$version-release-signed.apk
echo "APK successfully built."
