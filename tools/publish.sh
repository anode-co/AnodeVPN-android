#!/bin/bash

function publish() {
  local apk
  apk="${1}"

  local checksum
  checksum=$(sha256sum ${apk} | cut -d ' ' -f 1)

  local upload_url
  upload_url="$(curl \
    -H 'Content-Type: application/octet-stream' \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    https://api.github.com/repos/anode-co/AnodeVPN-android/releases 2>> /dev/null | \
    jq -r '.[] | .upload_url' | \
    head -n1)"
  upload_url=${upload_url/\{?name,label\}/}

  local release_name
  release_name="$(curl \
    -H 'Content-Type: application/octet-stream' \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    https://api.github.com/repos/anode-co/AnodeVPN-android/releases 2>> /dev/null | \
    jq -r '.[] | .tag_name' | \
    head -n1)"

  curl \
    -X POST \
    --data-binary @${apk} \
    -H 'Content-Type: application/octet-stream' \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    "${upload_url}?name=${release_name}.apk"

  curl \
    -X POST \
    --data "$checksum" \
    -H 'Content-Type: text/plain' \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    "${upload_url}?name=${release_name}.sha256sum"

  source /home/runner/work/AnodeVPN-android/AnodeVPN-android/tools/release_notify.sh
}
publish "/home/runner/work/AnodeVPN-android/AnodeVPN-android/app/build/outputs/apk/release/app-release.apk"