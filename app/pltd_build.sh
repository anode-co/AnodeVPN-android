echo 'Building pltd emulator'
export ANDROID_NDK_HOME=$HOME/Android/Ndk
export PATH=$PATH:$HOME/go/bin
cd ~/Code/pktd
PKTD_PATH=$HOME/Code/pktd
export ANODIUM_LIBS=$HOME/Code/AnodeVPN-android/app/src/main/jniLibs
GOMOBILE="$HOME/go/pkg/gomobile" GOOS=android GOARCH=386 CC=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/x86_64-linux-android29-clang CXX=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/x86_64-linux-android29-clang++ CGO_ENABLED=1 GOARM=7 go build -p=8 -pkgdir=$GOMOBILE/pkg_android_arm -tags="" -ldflags="-s -w -extldflags=-pie" -o $ANODIUM_LIBS/x86/libpltd.so -x $PKTD_PATH/lnd/cmd/lnd/main.go
echo 'Building pltd aarch64'
GOMOBILE="$HOME/go/pkg/gomobile" GOOS=android GOARCH=arm64 CC=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android29-clang CXX=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android29-clang++ CGO_ENABLED=1 GOARM=7 go build -p=8 -pkgdir=$GOMOBILE/pkg_android_arm -tags="" -ldflags="-s -w -extldflags=-pie" -o $ANODIUM_LIBS/arm64-v8a/libpltd.so -x $PKTD_PATH/lnd/cmd/lnd/main.go
echo 'Building pltd armv7a'
GOMOBILE="$HOME/go/pkg/gomobile" GOOS=android GOARCH=arm CC=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/armv7a-linux-androideabi29-clang CXX=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/armv7a-linux-androideabi29-clang++ CGO_ENABLED=1 GOARM=7 go build -p=8 -pkgdir=$GOMOBILE/pkg_android_arm -tags="" -ldflags="-s -w -extldflags=-pie" -o $ANODIUM_LIBS/armeabi-v7a/libpltd.so -x $PKTD_PATH/lnd/cmd/lnd/main.go