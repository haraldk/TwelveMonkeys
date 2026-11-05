# Patched JXLatte (temporary)

A file based Maven repository holding a patched build of [JXLatte](https://github.com/Traneptora/jxlatte)
(`com.traneptora:jxlatte:0.1.0-tm.4`), used by the `imageio-jxl` module until the changes are released upstream.
Delete this directory, and the `<repository>` in `../pom.xml`, once a JXLatte release with these changes is available.

JXLatte is Copyright 2022-2024 Leo Izen (Traneptora), MIT License, see `LICENSE-jxlatte.txt`.

## Contents

JXLatte `main` (`61742c3`) plus these changes, each on its own branch for upstream pull requests:

| Branch                     | Commit    | Change |
|----------------------------|-----------|--------|
| `fix-truncated-input-hang` | `d2a0a17` | Don't block forever on truncated input |
| `fix-pq-transfer-function` | `f811bc1` | Fix the PQ (SMPTE ST 2084) transfer function |
| `fix-mathhelper-max-float` | `038f913` | Fix `MathHelper.max(float...)` returning the minimum |
| `fix-untrusted-allocations` | `a748e3e` | Don't allocate buffers from untrusted lengths before reading the data |
| `add-header-api`           | `92a98bc` | `JXLDecoder.readHeader()`, read the image header without decoding the image (required by `imageio-jxl`) |
| `#50-add-hlg-transfer`     | `bf6a63d` | Add the HLG (Hybrid Log-Gamma) transfer function |
| `fix-lossless-float`       | `fd6f659` | Fix decoding of lossless 32-bit float images (64-bit predictions, float sample bits) |

Built from the merge of all of these, the local JXLatte branch `local-fixes` (`ecb4555`).

## How it was built

```sh
# In the JXLatte checkout, on branch local-fixes
mvn verify -DskipTests -Dmaven.javadoc.skip=true

# Same pom, with the version changed to 0.1.0-tm.4 (pom-tm.xml)
mvn deploy:deploy-file -Dfile=target/jxlatte-0.1.0.jar -Dsources=target/jxlatte-0.1.0-sources.jar \
    -DpomFile=pom-tm.xml -DrepositoryId=imageio-jxl-lib -Durl=file://<path to>/imageio/imageio-jxl/lib
```

When updating, increase the version suffix (`-tm.5`, ...) instead of replacing the jar, as Maven caches resolved
artifacts in `~/.m2` by version. Remove the old version (and redeploy, to update `maven-metadata.xml`).
