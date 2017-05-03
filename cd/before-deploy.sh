#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" == 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $encrypted_6061f4efda3d_key -iv $encrypted_6061f4efda3d_iv -in cd/codesigning.asc.enc -out cd/codesigning.asc -d
    gpg --fast-import cd/codesigning.asc
fi
