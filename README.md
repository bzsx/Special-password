<!-- markdownlint-disable html -->
<!-- markdownlint-disable no-duplicate-header -->

<div align="center">
  <img src="ic_launcher.png" width="120px" alt="神奇的密码图标" style="border-radius: 20px;">
  <h1>✨ 神奇的密码</h1>
  <p>纯本地离线 · AES-256-GCM 加密 · Material 3 设计</p>
</div>

<div align="center" style="line-height: 1.8;">
  <!-- 平台 -->
  <a href="https://www.android.com/">
    <img alt="Platform" src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  </a>
  <!-- 语言 -->
  <a href="https://www.java.com/">
    <img alt="Language" src="https://img.shields.io/badge/Language-Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  </a>
  <!-- 协议 -->
  <a href="https://github.com/bzsx/bzsx_password/blob/main/LICENSE">
    <img alt="License" src="https://img.shields.io/badge/License-AGPL%203.0-blue?style=for-the-badge"/>
  </a>
  <br>
  <!-- UI 框架 -->
  <a href="https://m3.material.io/">
    <img alt="UI" src="https://img.shields.io/badge/UI-Material%203-757575?style=for-the-badge&logo=materialdesign&logoColor=white"/>
  </a>
  <!-- 加密方案 -->
  <a href="#-安全与加密">
    <img alt="Encryption" src="https://img.shields.io/badge/Encryption-AES--256--GCM-success?style=for-the-badge"/>
  </a>
  <!-- 隐私 -->
  <a href="#-简介">
    <img alt="Privacy" src="https://img.shields.io/badge/Privacy-100%25%20Offline-4CAF50?style=for-the-badge"/>
  </a>
  <br>
  <!-- 作者 B站 -->
  <a href="https://space.bilibili.com/3546612747995937">
    <img alt="Bilibili" src="https://img.shields.io/badge/B站-宝藏水仙-00A1D6?style=for-the-badge&logo=bilibili&logoColor=white"/>
  </a>
  <!-- GitHub -->
  <a href="https://github.com/bzsx/bzsx_password">
    <img alt="GitHub" src="https://img.shields.io/badge/GitHub-项目地址-181717?style=for-the-badge&logo=github&logoColor=white"/>
  </a>
</div>

---

## 📑 目录

- [📖 简介](#-简介)
- [✨ 功能特性](#-功能特性)
  - [🔐 安全与加密](#-安全与加密)
  - [📁 密码管理](#-密码管理)
  - [📤 导入与导出](#-导入与导出)
  - [🎨 界面与体验](#-界面与体验)
- [📦 下载与安装](#-下载与安装)
- [🔍 检查更新](#-检查更新)
- [👤 关于作者](#-关于作者)
- [📄 许可证](#-许可证)

---

## 📖 简介

这是一款无需联网、不上传任何数据的密码管理器。所有密码数据均存储在手机本地，采用 AES-GCM 加密算法保护你的隐私安全。支持密码的增删改查、导入导出、搜索排序、批量管理等完整功能，界面采用 Material 3 设计语言，流畅美观。


## ✨ 功能特性

### 🔐 安全与加密
- 主密码锁屏保护，每次打开应用需验证
- **支持二选一解锁方式**：应用主密码 / 手机系统锁屏密码，忘记密码可通过系统锁屏验证找回
- 采用 **AES-256-GCM + PBKDF2** 加密方案（v1.2 起），主密码经 PBKDF2（100,000 次迭代 + 随机盐）派生密钥，不存储明文
- 支持修改主密码
- 密码强度实时检测（弱/中/强/超强/离谱强）

### 📁 密码管理
- 添加、编辑、删除密码条目
- 密码列表卡片显示，支持拖动排序
- 置顶功能（置顶卡片独立排序，蓝色标签标识）
- 批量选择（长按进入选择模式，支持全选/批量删除）
- 搜索功能（实时过滤，匹配文字高亮显示）
- 空状态提示（无密码 / 搜索无结果）
- 列表显示密码总数及最后修改时间

### 📤 导入与导出
- 导出密码为 AES-GCM 加密备份文件
- 导入备份文件时支持选择置顶或非置顶方式
- 导入对话框采用 CheckBox 选择，操作直观

### 🎨 界面与体验
- Material 3 设计语言（动态配色、弹性动效、大圆角卡片）
- 主题色自定义（6 种预选颜色 + RGB 手动调色）
- 沉浸式状态栏，Toolbar 跟随主题色
- 详情页预览返回手势（左边缘拖拽预览上一页）
- 开屏动画（打字机效果，可跳过，设置页可开关）
- 检查更新支持一键下载安装，实时显示进度条、网速和文件大小
- 关于页新增"联系作者"弹窗（B站、QQ群、邮箱）及"前往 GitHub 项目"入口


## 📦 下载与安装

- **最新版本**：v1.3.6（正式版）
- **下载地址**：[点击下载 APK](https://223-109-125-226-v3.pd1.cjjd19.com/user-other-free-download-cdn.123295.com/123-678/a48fec14/1853663930-0/a48fec141a003dd2222f866ca6dceea6/c-m8012?bi=162747709&r=RWT4WA&t=1787052123&bzc=1&bzs=313835333636333933303a323a313a36313336313837&bzp=0&ur=aalgvnugpegpe&urn=0&v=5&s=178705212386e00115ce347f1915199f8371ae5257&filename=%E7%A5%9E%E5%A5%87%E7%9A%84%E5%AF%86%E7%A0%81_1.3.6.apk&cache_type=2&x-mf-biz-cid=15ae584c-1d5e-4cf8-a77b-370a86e941a5&auto_redirect=0&ndcp=1)
- **系统要求**：Android 7.0 及以上


## 🔍 检查更新

**更新检查地址**：[https://bzsx.lhx520.icu/password.html](https://bzsx.lhx520.icu/password.html)


## 👤 关于作者

- **制作**：宝藏水仙
- **个人主页**：[https://bzsx.lhx520.icu](https://bzsx.lhx520.icu)
- **B站空间**：[宝藏水仙的B站主页](https://space.bilibili.com/3546612747995937)


## 📄 许可证

本项目采用 **GNU Affero General Public License v3.0 (AGPL-3.0)** 开源协议。

你可以自由地使用、学习、修改和分发本项目的源代码，但必须遵守以下核心条款：

1. **禁止商用**：你**不可以**将本软件或其修改版本用于任何形式的商业目的（包括但不限于销售、收费服务、广告盈利等）。
2. **源代码归属**：本项目的源代码所有权和知识产权归 **宝藏水仙** 所有。
3. **专利授权**：如果你对本项目有任何贡献，你自动授予项目作者和所有使用者一项永久的、全球性的、免费的专利许可，但你不能利用本项目的代码来发起任何专利诉讼。
4. **开源传承**：如果你修改或基于本项目衍生了新的软件，你**必须**同样使用 `AGPL-3.0` 协议开源，并保留原始的版权和许可声明。

完整的协议文本请参见项目根目录下的 `LICENSE` 文件。

> **特别声明**：本项目仅限个人学习、研究和非商业用途。任何商业使用行为均需获得作者 **宝藏水仙** 的明确书面授权。


---
*Made with ❤️ by 宝藏水仙*