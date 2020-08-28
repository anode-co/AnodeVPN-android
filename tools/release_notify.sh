#!/usr/bin/env bash
die() { echo "$1"; exit 1; }
if test "x$ANODE_APIKEY" = "x"; then
  die "No API key present"
fi

file=$(mktemp)
GIT_ID=$(git describe --tags HEAD)
echo "$GIT_ID" | sed 's/anodium-\|\.\|-/\n/g' | awk '
  BEGIN {
    M=-1;
    m=-1;
    p=-1;
  }
  {
    if ($0 !~ /^[0-9]+$/) {
    } else if (M == -1) {
      M = $0;
    } else if (m == -1) {
      m = $0;
    } else if (p == -1) {
      p = $0;
    }
  }
  END {
    print("MAJOR="M"\nMINOR="m"\nPATCH="p);
  }' > "$file"

source "$file"
rm "$file"

DOWNLOAD_URL="https://github.com/anode-co/AnodeVPN-android/releases/download/anodevpn-$MAJOR.$MINOR.$PATCH/app-release.apk"
CERT_URL="https://anode.co/nonexistant/nonesuch"

echo "{\"clientOs\":\"android\",\"majorNumber\":$MAJOR,\"minorNumber\":$MINOR,\"revisionNumber\":$PATCH,\"clientSoftwareVersion\":\"android-0.0.1\",\"binaryDownloadUrl\":\"$DOWNLOAD_URL\",\"certificateUrl\":\"$CERT_URL\"}"
echo
curl -X POST "https://vpn.anode.co/api/0.3/vpn/clients/versions/android/" \
  -H "accept: */*" \
  -H "Content-Type: application/json" \
  -H "Authorization: Api-Key $ANODE_APIKEY" \
  -d "{\"clientOs\":\"android\",\"majorNumber\":$MAJOR,\"minorNumber\":$MINOR,\"revisionNumber\":$PATCH,\"clientSoftwareVersion\":\"android-0.0.1\",\"binaryDownloadUrl\":\"$DOWNLOAD_URL\",\"certificateUrl\":\"$CERT_URL\"}"