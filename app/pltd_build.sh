echo 'Building pltd emulator'
export ANDROID_NDK_HOME=$HOME/Code/cjdns/build_android/android-ndk-r21
export PATH=$PATH:$HOME/go/bin
cd ~/Code/pktd
PKTD_PATH=$HOME/Code/pktd
ANODIUM_ASSETS=$HOME/Code/AnodeVPN-android/app/src/main/assets
GOMOBILE="$HOME/Applications/go/pkg/gomobile" GOOS=android GOARCH=386 CC=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/x86_64-linux-android21-clang CXX=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/x86_64-linux-android21-clang++ CGO_ENABLED=1 GOARM=7 go build -p=8 -pkgdir=$GOMOBILE/pkg_android_arm -tags="" -ldflags="-s -w -extldflags=-pie" -o $ANODIUM_ASSETS/x86_64/pltd -x $PKTD_PATH/lnd/cmd/lnd/main.go
echo 'Compressing pltd emulator'
echo 'Building pltd aarch64'
cd $ANODIUM_ASSETS/x86_64
~/Applications/upx-3.96-amd64_linux/upx pltd
cd ~/Code/pktd
GOMOBILE="$HOME/Applications/go/pkg/gomobile" GOOS=android GOARCH=arm64 CC=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android21-clang CXX=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android28-clang++ CGO_ENABLED=1 GOARM=7 go build -p=8 -pkgdir=$GOMOBILE/pkg_android_arm -tags="" -ldflags="-s -w -extldflags=-pie" -o $ANODIUM_ASSETS/aarch64/pltd -x $PKTD_PATH/lnd/cmd/lnd/main.go
echo 'Compressing pltd aarch64'
cd $ANODIUM_ASSETS/aarch64
~/Applications/upx-3.96-amd64_linux/upx pltd