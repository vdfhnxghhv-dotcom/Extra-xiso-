#!/data/data/com.termux/files/usr/bin/bash

echo "Building AAB..."

# 1. تنظيف المجلدات القديمة
rm -rf build_aab
mkdir -p build_aab/gen build_aab/obj
mkdir -p build_aab/base/manifest build_aab/base/dex build_aab/base/res build_aab/base/lib/arm64-v8a

# 2. تجميع الموارد بصيغة Protobuf (هام جداً)
echo "Compiling resources (protobuf)..."
aapt2 compile --dir src/main/res -o build_aab/res.zip
# لاحظ استخدام --proto-format هنا
aapt2 link -o build_aab/base.zip -I android.jar --manifest src/main/AndroidManifest.xml -R build_aab/res.zip --java build_aab/gen --auto-add-overlay --proto-format

# 3. استخراج الموارد وتجهيز هيكل AAB
echo "Extracting proto resources..."
unzip -q build_aab/base.zip -d build_aab/base_extracted
mv build_aab/base_extracted/AndroidManifest.xml build_aab/base/manifest/
mv build_aab/base_extracted/resources.pb build_aab/base/
mv build_aab/base_extracted/res build_aab/base/

# 4. تجميع الجافا
echo "Compiling Java..."
javac -g --release 8 -cp android.jar -d build_aab/obj build_aab/gen/com/example/myapp/R.java src/main/java/com/example/myapp/MainActivity.java

# 5. تحويل إلى DEX
echo "Converting to DEX..."
d8 --min-api 21 --lib android.jar --output build_aab/base/dex build_aab/obj/com/example/myapp/*.class

# 6. نسخ المكتبة الأصلية
echo "Copying native libs..."
cp src/main/lib/arm64-v8a/libextract-xiso.so build_aab/base/lib/arm64-v8a/

# 7. ضغط هيكل AAB في ملف base.zip
echo "Packaging AAB module..."
cd build_aab/base
zip -r -0 ../base_module.zip .
cd ../..

# 8. بناء ملف AAB باستخدام bundletool
echo "Building AAB with bundletool..."
java -jar bundletool.jar build-bundle --modules=build_aab/base_module.zip --output=build_aab/app.aab

# 9. توقيع ملف AAB
echo "Signing AAB..."
jarsigner -sigalg SHA256withRSA -digestalg SHA-256 -keystore debug.keystore -storepass android -keypass android build_aab/app.aab androiddebugkey

echo "Done! Your AAB is at: /storage/emulated/0/app/build_aab/app.aab"
