# 跨平台打包矩阵

本参考文档提供 jpackage 选项、工具链、图标格式、代码签名以及 CI/CD 构建矩阵的并排对比，这些是向 Windows、macOS 和 Linux 发布 JavaFX 应用所必需的。它补充了 `javafx-developer/SKILL.md` 和 `_shared/packaging-reference.md` 中的打包章节，后者定义了核心约束；本文档侧重于**跨平台的差异**以及从单一代码库产出三种安装包的**构建矩阵策略**。

> **核心约束**：`jpackage` 无法交叉编译。每个平台的安装包必须在运行该操作系统的宿主机上产出。因此多平台交付是一个 CI/CD 矩阵问题，而非单条命令能解决的问题。

---

## 1. 各平台专属的 jpackage 选项矩阵

下表将每个平台映射到其输出类型以及该平台专属（或最相关）的 jpackage 选项。通用选项（`--name`、`--app-version`、`--vendor`、`--module`、`--module-path`、`--runtime-image`、`--java-options`、`--dest`）适用于所有平台，故从表中省略。

| 关注点 | Windows | macOS | Linux |
|---------|---------|-------|-------|
| 输出 `--type` | `msi`（WiX）/ `exe`（Inno Setup） | `dmg` / `pkg` | `deb` / `rpm` |
| 图标 `--icon` | `.ico`（多尺寸内嵌） | `.icns` | `.png` |
| 包名 | `--name` | `--mac-package-name` | `--linux-package-name` |
| 包标识符 | `--win-upgrade-uuid`（UUID v4） | `--mac-package-identifier`（反向 DNS） | `--linux-deb-maintainer`（邮箱） |
| 快捷方式 / 菜单 | `--win-menu`、`--win-shortcut`、`--win-dir-chooser`、`--win-per-user-install` | 由 `.app` 包管理 | 由桌面入口管理 |
| 代码签名 | 外部 `signtool` / WiX resource-dir | `--mac-sign`、`--mac-signing-key-user-name` | 通过 `dpkg-sig` / `rpm --addsign` 的 GPG |
| 许可证元数据 | 通过 WiX 模板 | 通过 `Info.plist` | `--linux-rpm-license-type` |
| 安装目录 | `--install-dir` | `.app` 包约定 | `--install-dir` |

### 1.1 各平台最小命令模板

```bash
# Windows (.msi) — requires WiX Toolset 4.x
jpackage --type msi --name "MyApp" --app-version 1.0.0 --vendor "MyCompany" \
  --module com.example/com.example.App --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icons/app.ico \
  --win-menu --win-shortcut \
  --win-upgrade-uuid "12345678-1234-4234-8234-123456789abc" \
  --java-options "--enable-native-access=javafx.graphics" --dest dist

# macOS (.dmg) — requires Xcode command line tools
jpackage --type dmg --name "MyApp" --app-version 1.0.0 --vendor "MyCompany" \
  --module com.example/com.example.App --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icons/app.icns \
  --mac-package-name "MyApp" \
  --mac-package-identifier "com.mycompany.myapp" \
  --mac-sign --mac-signing-key-user-name "Developer ID Application: Your Name (XXXXXXXXXX)" \
  --java-options "--enable-native-access=javafx.graphics" --dest dist

# Linux (.deb) — requires dpkg-deb
jpackage --type deb --name "myapp" --app-version 1.0.0 --vendor "MyCompany" \
  --module com.example/com.example.App --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icons/app.png \
  --linux-package-name "myapp" \
  --linux-deb-maintainer "dev@mycompany.com" \
  --linux-rpm-license-type "MIT" --install-dir /opt/myapp \
  --java-options "--enable-native-access=javafx.graphics" --dest dist

# Linux (.rpm) — requires rpmbuild
jpackage --type rpm --name "myapp" --app-version 1.0.0 --vendor "MyCompany" \
  --module com.example/com.example.App --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icons/app.png \
  --linux-package-name "myapp" \
  --linux-rpm-license-type "MIT" --install-dir /opt/myapp \
  --java-options "--enable-native-access=javafx.graphics" --dest dist
```

> 完整的各平台命令模板（包括快捷方式标志和签名）也内嵌于 `javafx-developer` 的 SKILL.md 打包章节和 `_shared/packaging-reference.md` 中。完整的参数参考和 jlink 运行时镜像生成请参见 `_shared/packaging-reference.md`。

---

## 2. 各平台工具链要求

jpackage 会调用各平台独有的原生打包工具链。该工具链必须存在于产出相应安装包的构建宿主机上。

| 平台 | 输出类型 | 必需工具链 | 安装方式 | 验证命令 |
|----------|-------------|---------------------|-----------------|----------------------|
| Windows | `msi` | WiX Toolset 4.x | `dotnet tool install --global wix` | `wix --version` |
| Windows | `exe` | Inno Setup 6+ | 从 jrsoftware.org 下载，添加到 `PATH` | `iscc /?` |
| macOS | `dmg` / `pkg` | Xcode 命令行工具 | `xcode-select --install` | `xcode-select -p` |
| macOS | 签名 | Developer ID Application 证书 | Apple Developer 门户 | `security find-identity -p codesigning` |
| Linux | `deb` | `dpkg-deb` | `apt install dpkg-dev`（Debian/Ubuntu） | `dpkg-deb --version` |
| Linux | `rpm` | `rpmbuild` | `dnf install rpm-build`（Fedora/RHEL） | `rpmbuild --version` |
| Linux | 签名 | `dpkg-sig` / `rpm` GPG | `apt install dpkg-sig` | `dpkg-sig --help` |

### 2.1 工具链检测说明

- **Windows**：WiX 4.x 作为 `.NET` 全局工具安装；确保 `$HOME/.dotnet/tools` 位于 `PATH` 中。Inno Setup 将 `ISCC.exe` 安装到其安装目录——需手动将该目录添加到 `PATH`。
- **macOS**：无需完整安装 Xcode；命令行工具对 `jpackage` 已足够。公证还需要 App Store Connect API 密钥或应用专用密码。
- **Linux**：如果同时安装了 `dpkg-deb` 和 `rpmbuild`（例如同时包含两者的容器），单个宿主机即可产出 `.deb` 和 `.rpm`。只要二进制存在，跨发行版工具链可在任何 Linux 发行版上工作。

---

## 3. 图标格式规范与转换指南

每个平台需要不同的图标格式。jpackage 会拒绝不匹配的格式，因此必须为每个平台提供正确的文件。

| 平台 | 格式 | 推荐尺寸 | 必需的内嵌尺寸 |
|----------|--------|-------------------|-------------------------|
| Windows | `.ico` | 256x256 基准 | 16、32、48、256（多尺寸内嵌） |
| macOS | `.icns` | 1024x1024 基准 | 16、32、64、128、256、512、1024（含 @2x） |
| Linux | `.png` | 512x512 | 单个文件即可 |

### 3.1 源文件

从一个单一的高分辨率源（1024x1024 PNG 或 SVG）开始。从该源派生出所有三个平台的图标，以保持品牌一致性。

### 3.2 生成 Windows `.ico`

使用 ImageMagick 或 `png2ico` 将多个 PNG 尺寸合并到一个 `.ico`：

```bash
# ImageMagick: generate multi-size .ico from a single source
magick icon1024.png -define icon:auto-resize=256,48,32,16 assets/icons/app.ico
```

### 3.3 生成 macOS `.icns`

使用内置的 `iconutil` 命令（仅限 macOS）将 `.iconset` 组装成 `.icns`：

```bash
mkdir MyApp.iconset
sips -z 16 16   icon1024.png --out MyApp.iconset/icon_16x16.png
sips -z 32 32   icon1024.png --out MyApp.iconset/icon_16x16@2x.png
sips -z 32 32   icon1024.png --out MyApp.iconset/icon_32x32.png
sips -z 64 64   icon1024.png --out MyApp.iconset/icon_32x32@2x.png
sips -z 128 128 icon1024.png --out MyApp.iconset/icon_128x128.png
sips -z 256 256 icon1024.png --out MyApp.iconset/icon_128x128@2x.png
sips -z 256 256 icon1024.png --out MyApp.iconset/icon_256x256.png
sips -z 512 512 icon1024.png --out MyApp.iconset/icon_256x256@2x.png
sips -z 512 512 icon1024.png --out MyApp.iconset/icon_512x512.png
cp icon1024.png          MyApp.iconset/icon_512x512@2x.png

iconutil -c icns MyApp.iconset
```

> `.icns` 步骤在 macOS 上运行。对于跨平台 CI，请将预构建的 `.icns` 提交到仓库，而不是在每次构建时重新生成。

### 3.4 Linux `.png`

单个 512x512 PNG 即可。无需转换工具；用 `sips` 或 ImageMagick 对源图进行缩小：

```bash
magick icon1024.png -resize 512x512 assets/icons/app.png
```

---

## 4. 代码签名概述

代码签名可证明安装包来源于可信源，并防止操作系统的安全警告（Windows SmartScreen、macOS Gatekeeper）。

### 4.1 Windows（Authenticode）

jpackage 本身不会对最终安装包签名；请在产物生成后使用 `signtool` 对 `.msi`/`.exe` 进行签名（需要代码签名证书——OV 或 EV）：

```bash
signtool sign /fd SHA256 /a /tr http://timestamp.digicert.com /td SHA256 \
  dist\MyApp-1.0.0.msi
```

- **证书类型**：OV（组织验证）会触发 SmartScreen 信誉建立；EV（扩展验证）立即受信任，但需要硬件令牌。
- **替代方案**：在 CI 中进行跨平台签名时使用 `osslsigncode`。
- **WiX 集成**：签名也可以通过放入 `--resource-dir` 的 WiX 自定义模板来接线。

### 4.2 macOS（签名 + 公证）

macOS 分发需要**签名**和**公证**两者，否则 Gatekeeper 会在首次打开时阻止应用。

```bash
# 1. Sign during jpackage
jpackage --type dmg --name MyApp \
  --mac-sign \
  --mac-signing-key-user-name "Developer ID Application: Your Name (XXXXXXXXXX)" \
  ...other args...

# 2. Submit for notarization
xcrun notarytool submit dist/MyApp-1.0.0.dmg \
  --apple-id "your@email.com" \
  --password "app-specific-password" \
  --team-id "XXXXXXXXXX" --wait

# 3. Staple the notarization ticket
xcrun stapler staple dist/MyApp-1.0.0.dmg
```

- **要求**：来自 Apple Developer Program 的 Developer ID Application 证书。
- **应用专用密码**：在 appleid.apple.com 为 `notarytool` 生成一个。
- **通用二进制（Universal Binary）**：为支持 Apple Silicon + Intel，构建 x64 和 aarch64 的 app-image，用 `lipo` 合并，然后对合并后的包进行签名和公证。

### 4.3 Linux（GPG）

Linux `.deb`/`.rpm` 包通常用 GPG 签名（可选，但对仓库推荐）：

```bash
# Sign deb
dpkg-sig --sign builder dist/myapp-1.0.0.deb

# Sign rpm (requires signing key configured in ~/.rpmmacros)
rpm --addsign dist/myapp-1.0.0.rpm
```

### 4.4 签名能力矩阵

| 平台 | 机制 | 所需凭据 | 是否强制？ |
|----------|-----------|---------------------|------------|
| Windows | Authenticode（`signtool`） | 代码签名证书（OV/EV） | 否（无证书会出现 SmartScreen 警告） |
| macOS | Developer ID + 公证 | Developer ID Application 证书 + App Store Connect | 是（Gatekeeper 会阻止未签名的应用） |
| Linux | GPG | GPG 签名密钥 | 否（仅用于 APT/YUM 仓库） |

---

## 5. CI/CD 多平台构建矩阵策略

由于 jpackage 无法交叉编译，多平台交付通过**矩阵构建**实现，即在 Windows、macOS 和 Linux 运行器上并行运行同一作业，每个作业产出其自身操作系统的产物。

### 5.1 矩阵策略原则

1. **每平台一个作业**：矩阵维度 `os: [windows-latest, macos-latest, ubuntu-latest]` 扇出三个并行作业。
2. **条件化工具链安装**：每个作业只安装其平台所需的工具链（`if:` 守卫）。
3. **每平台 JavaFX SDK**：下载与运行器匹配的 SDK 分类器（`windows`、`mac`、`linux`）。
4. **单一版本来源**：从 Git 标签派生 `--app-version`，使三个产物共享一个版本。
5. **每平台上载产物**：以带平台标签的名称上载每个平台的安装包。
6. **发布步骤**：将全部产物附加到同一个 GitHub/GitLab release。

### 5.2 GitHub Actions 矩阵骨架

```yaml
name: Build and Package

on:
  push:
    tags: ['v*']
  workflow_dispatch:

jobs:
  package:
    strategy:
      matrix:
        os: [windows-latest, macos-latest, ubuntu-latest]
        include:
          - os: windows-latest
            platform: windows
            artifact: MyApp-*.msi
            icon: assets/icons/app.ico
          - os: macos-latest
            platform: mac
            artifact: MyApp-*.dmg
            icon: assets/icons/app.icns
          - os: ubuntu-latest
            platform: linux
            artifact: myapp_*.deb
            icon: assets/icons/app.png

    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: Setup JavaFX SDK
        run: |
          curl -L "https://download2.gluonhq.com/openjfx/21.0.11/openjfx-21.0.11_${{ matrix.platform }}-x64_bin-sdk.zip" -o javafx-sdk.zip
          if [ "$RUNNER_OS" = "Windows" ]; then 7z x javafx-sdk.zip -ojavafx-sdk; else unzip javafx-sdk.zip -d javafx-sdk; fi
          echo "JAVAFX_SDK=javafx-sdk/javafx-sdk-21.0.11/lib" >> $GITHUB_ENV
        shell: bash

      - name: Install WiX (Windows only)
        if: matrix.platform == 'windows'
        run: |
          dotnet tool install --global wix
          echo "$HOME/.dotnet/tools" >> $GITHUB_PATH

      - name: Build JAR
        run: mvn -B clean package -DskipTests

      - name: Create jlink runtime image
        run: |
          jlink --module-path "target/modules:$JAVAFX_SDK" \
            --add-modules com.example --output build/runtime \
            --strip-debug --compress zip-6 --no-header-files --no-man-pages
        shell: bash

      - name: Package (Windows)
        if: matrix.platform == 'windows'
        run: |
          jpackage --type msi --name MyApp --app-version ${GITHUB_REF_NAME} \
            --module com.example/com.example.App --module-path "target/modules:$JAVAFX_SDK" \
            --runtime-image build/runtime --icon ${{ matrix.icon }} \
            --win-menu --win-shortcut \
            --win-upgrade-uuid "12345678-1234-4234-8234-123456789abc" \
            --java-options "--enable-native-access=javafx.graphics" --dest dist

      - name: Package (macOS)
        if: matrix.platform == 'mac'
        run: |
          jpackage --type dmg --name MyApp --app-version ${GITHUB_REF_NAME} \
            --module com.example/com.example.App --module-path "target/modules:$JAVAFX_SDK" \
            --runtime-image build/runtime --icon ${{ matrix.icon }} \
            --mac-package-identifier com.mycompany.myapp \
            --java-options "--enable-native-access=javafx.graphics" --dest dist

      - name: Package (Linux)
        if: matrix.platform == 'linux'
        run: |
          jpackage --type deb --name myapp --app-version ${GITHUB_REF_NAME} \
            --module com.example/com.example.App --module-path "target/modules:$JAVAFX_SDK" \
            --runtime-image build/runtime --icon ${{ matrix.icon }} \
            --linux-deb-maintainer "dev@mycompany.com" \
            --java-options "--enable-native-access=javafx.graphics" --dest dist

      - uses: actions/upload-artifact@v4
        with:
          name: MyApp-${{ matrix.platform }}
          path: dist/${{ matrix.artifact }}

      - name: Release
        if: startsWith(github.ref, 'refs/tags/')
        uses: softprops/action-gh-release@v2
        with:
          files: dist/${{ matrix.artifact }}
```

### 5.3 矩阵构建说明

- **交叉编译限制**：切勿尝试在 Linux 运行器上生成 Windows `.msi`——这会失败。每个产物必须来自其原生运行器。
- **缓存**：缓存 Maven/Gradle 依赖和 JavaFX SDK 下载，以加速矩阵作业。
- **macOS 通用二进制**：为支持 Apple Silicon + Intel，添加一个构建合并 `lipo` 包的第二个 macOS 矩阵条目，或生成两个单独的 `.dmg` 产物。
- **密钥管理**：将签名证书、Apple 凭据和 GPG 密钥存储为 CI 密钥；切勿提交到仓库。
- **验证钩子**：`runtime verification` 技能可在矩阵中按平台调用，以验证每个产物（当前平台可执行检查 + 非当前平台配置完整性检查）。

---

## 6. 跨平台打包检查清单

在触发多平台矩阵构建之前，请确认每个平台的配置都是完整的。`runtime verification` 的打包维度通过配置完整性来验证这一点（见打包验证维度）。

| # | 检查项 | Windows | macOS | Linux |
|---|-------|---------|-------|-------|
| 1 | 定义了输出 `--type` | `msi`/`exe` | `dmg`/`pkg` | `deb`/`rpm` |
| 2 | 图标文件存在且格式正确 | `.ico` | `.icns` | `.png` |
| 3 | 存在包名 / 标识符 | `--win-upgrade-uuid`（UUID v4） | `--mac-package-name` + `--mac-package-identifier` | `--linux-package-name` + `--linux-deb-maintainer` |
| 4 | 目标运行器上工具链可用 | WiX / Inno Setup | Xcode 工具 | `dpkg-deb` / `rpmbuild` |
| 5 | JavaFX SDK 分类器与平台匹配 | `windows` | `mac` | `linux` |
| 6 | `--java-options "--enable-native-access=javafx.graphics"`（JavaFX 24+） | 是 | 是 | 是 |
| 7 | 已配置代码签名 | `signtool` + 证书 | `--mac-sign` + 公证 | GPG（可选） |
| 8 | 版本来源于单一标签 | 是 | 是 | 是 |

> **提醒**：只定义了 Windows `msi` 命令的项目会在 Windows 上通过打包验证，但在"跨平台配置完整性"检查中失败。在交付多平台发布之前，请完成所有三个平台的配置。
