#!/bin/bash

function publish() {
  local archive
  archive="${1}"

  local checksum
  checksum=$(sha256sum ${archive} | cut -d ' ' -f 1)

  local event_data
  # See https://docs.github.com/en/actions/reference/environment-variables
  event_data=$(cat "$GITHUB_EVENT_PATH")

  local upload_url
  upload_url=$(echo "$event_data" | jq -r .release.upload_url)
  upload_url=${upload_url/\{?name,label\}/}

  local release_name
  release_name=$(echo "$event_data" | jq -r .release.tag_name)

  local project_name
  project_name=$(basename "$GITHUB_REPOSITORY")

  local name
  name="${name:-${project_name}_${release_name}}"

  curl \
    -X POST \
    --data-binary @${archive} \
    -H 'Content-Type: application/octet-stream' \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    "${upload_url}?name=${name}.${archive/tmp./}"

  curl \
    -X POST \
    --data "$checksum" \
    -H 'Content-Type: text/plain' \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    "${upload_url}?name=${name}.sha256sum"

  source anode-co/AnodeVPN-android/release_notify.sh
}
publish "anode-co/AnodeVPN-android/app-release.apk"