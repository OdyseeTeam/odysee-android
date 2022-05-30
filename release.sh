#!/bin/bash
./gradlew assembleRelease
version=$(./gradlew -q printVersionName --console=plain | tail -1)
mkdir -p bin/
rm -f bin/*
cp app/build/outputs/apk/full/release/app-full-release-unsigned.apk bin/odysee-$version-release-unsigned.apk

# sign APK
echo "Signing APK..."
zipalign -v 4 \
    bin/odysee-$version-release-unsigned.apk bin/odysee-$version-release-unsigned-aligned.apk > /dev/null
apksigner sign \
    --ks lbry-android.keystore \
    --ks-pass pass:$KEYSTORE_PASSWORD \
    --v1-signing-enabled true \
    --v2-signing-enabled true \
    bin/odysee-$version-release-unsigned-aligned.apk > /dev/null \
    && mv bin/odysee-$version-release-unsigned-aligned.apk bin/odysee-$version-release.apk
echo "APK successfully built."
