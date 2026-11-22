#!/data/data/com.termux/files/usr/bin/bash

echo "Building App..."

# 1. Clean old build
rm -rf build
mkdir -p build/gen build/obj build/apk

# 2. Generate Keystore if not exists
if [ ! -f "debug.keystore" ]; then
    echo "Generating debug keystore..."
    keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

ANDROID_JAR="android.jar"

# 3. Compile Resources
echo "Compiling resources..."
aapt2 compile --dir src/main/res -o build/res.zip
aapt2 link -o build/app.unaligned.apk -I $ANDROID_JAR --manifest src/main/AndroidManifest.xml -R build/res.zip --java build/gen --auto-add-overlay

# 4. Compile Java files
echo "Compiling Java..."
javac -g --release 8 -cp $ANDROID_JAR -d build/obj build/gen/com/example/myapp/R.java src/main/java/com/example/myapp/MainActivity.java

# 5. Convert to DEX
echo "Converting to DEX..."
d8 --min-api 21 --lib $ANDROID_JAR --output build/apk build/obj/com/example/myapp/*.class

# 6. Package APK
echo "Packaging APK..."
cd build/apk
zip -u ../app.unaligned.apk classes.dex
cd ../..

cd src/main
zip -r ../../build/app.unaligned.apk assets
# إضافة مجلد lib إلى التطبيق
zip -r ../../build/app.unaligned.apk lib
cd ../..

# 7. Align APK
echo "Aligning APK..."
zipalign -f 4 build/app.unaligned.apk build/app.aligned.apk

# 8. Sign APK
echo "Signing APK..."
apksigner sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android --out build/app-final.apk build/app.aligned.apk

echo "Done! Your APK is at: /storage/emulated/0/app/build/app-final.apk"