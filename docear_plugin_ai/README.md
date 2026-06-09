# Docear AI 插件使用指南

Docear AI 插件为 Docear 思维导图软件提供了 AI 辅助功能。目前支持通过 **GitHub Copilot CLI** 调用 AI，帮助你快速生成子节点、分析研究思路等。

---

## 一、前置要求

- 有效的 **GitHub Copilot 订阅**（个人版 Pro、Business 或 Enterprise 均可）
- **Node.js**（用于安装 Copilot CLI）
- Docear 桌面版（已安装本插件）

---

## 二、安装 GitHub Copilot CLI

### 方式 1：使用 npm（推荐）

打开 PowerShell 或命令提示符，执行：

```powershell
npm install -g @github/copilot
```

### 方式 2：使用 WinGet（Windows）

```powershell
winget install --id GitHub.copilot
```

### 方式 3：使用 Homebrew（macOS / Linux）

```bash
brew install github/copilot/copilot-cli
```

安装完成后，验证是否成功：

```powershell
copilot --version
```

---

## 三、登录 GitHub Copilot 账号

首次运行 Copilot CLI 时需要登录：

```powershell
copilot
```

然后输入：

```
/login
```

按照提示完成以下任一认证方式：

### 1. OAuth Device Flow（推荐）

- 浏览器会打开 GitHub 授权页面
- 输入设备码完成登录
- 授权成功后，终端会显示 "Logged in as <你的用户名>"

### 2. 使用 GitHub CLI 已登录的账号

如果你已经安装并登录了 `gh` 命令行工具，Copilot CLI 会自动复用你的登录状态。

### 3. 使用 Personal Access Token（PAT）

1. 访问 https://github.com/settings/personal-access-tokens/new
2. 勾选权限：**Copilot Requests**
3. 生成 Token 后，通过环境变量设置：

```powershell
# 当前终端临时生效
$env:COPILOT_GITHUB_TOKEN="ghp_xxxxxxxxxxxx"

# 永久生效（推荐）
[Environment]::SetEnvironmentVariable("COPILOT_GITHUB_TOKEN", "ghp_xxxxxxxxxxxx", "User")
```

---

## 四、验证安装是否成功

执行以下命令测试：

```powershell
copilot -p "Hello, Copilot!" -s
```

如果返回一段自然语言文本，说明认证成功，Docear AI 插件即可正常使用。

---

## 五、在 Docear 中使用 AI 功能

### 生成子节点（当前支持的功能）

1. 打开任意思维导图
2. 选中一个节点
3. 右键点击 → 选择 `AI` → `生成子节点`
4. 插件会自动调用 Copilot CLI，生成 5 个子节点并插入到当前节点下

生成完成后，你可以像普通节点一样编辑、移动或删除它们。

---

## 六、故障排除

### 问题 1：`copilot` 命令找不到

**原因**：Node.js 全局路径未加入系统 PATH。

**解决**：
- 重新安装 Node.js，确保勾选 "Add to PATH"
- 或手动将 npm 全局目录加入 PATH（通常是 `C:\Users\<用户名>\AppData\Roaming\npm`）

### 问题 2：提示 "Copilot CLI is not available"

**原因**：插件检测到系统中没有 `copilot` 命令。

**解决**：按照本文「安装 GitHub Copilot CLI」步骤重新安装。

### 问题 3：提示需要组织管理员开启

**原因**：你是 GitHub Copilot Business / Enterprise 用户，组织策略未启用 CLI。

**解决**：联系组织管理员，在 GitHub 后台的 Policies 页面开启 "Copilot CLI" 权限。

### 问题 4：生成内容为空或超时

**原因**：Copilot CLI 响应慢或网络问题。

**解决**：
- 检查网络连接
- 尝试在终端手动执行 `copilot -p "测试"` 确认是否正常
- 插件内部已设置 120 秒超时，可稍后重试

---

## 七、未来扩展计划

本插件采用**可插拔架构**设计，后续将支持：

- OpenAI GPT-4o / GPT-4o-mini
- Claude 3.5 / 4
- 本地模型（Ollama、LM Studio）
- 更多 AI 功能：分析分支、改写内容、思维导图问答等

切换模型时，只需在 Docear「选项 → AI」面板中选择后端类型，无需修改代码。

---

## 八、反馈与贡献

如果你在使用过程中遇到问题，欢迎提交 Issue 或参与开发。

---

**祝你使用愉快！**  
Docear AI 团队