#!/bin/bash

function publish() {
  local apk
  apk="${1}"

  local checksum
  checksum=$(sha256sum ${apk} | cut -d ' ' -f 1)

  local event_data
  # See https://docs.github.com/en/actions/reference/environment-variables
  event_data=$(cat "$GITHUB_EVENT_PATH")

  local releases_url
  releases_url=$(echo "$event_data" | jq -r .releases_url)

  local release_event_response
  release_event_response="$(curl \
    -X GET \
    -H 'Content-Type: application/octet-stream' \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    "${releases_url}" 2>> /dev/null)"

  echo "${release_event_response}";

  local upload_url
  upload_url=$(echo "$event_data" | jq -r .release.upload_url)
  upload_url=${upload_url/\{?name,label\}/}

  local release_name
  release_name=$(echo "$event_data" | jq -r .release.tag_name)

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