# 🤖 VS Code AI チャット用 - 自動環境構築ガイド

> **使い方**: このファイルをVS CodeのAIチャット（GitHub Copilot Chat）に読み込ませて、「このガイドに従って環境構築を自動で実行してください」と依頼してください。

## 📋 プロジェクト概要

```json
{
  "project_name": "chatapp",
  "repository_url": "https://github.com/SuYu0513/newchatapp.git",
  "project_type": "Spring Boot Web Application",
  "language": "Java",
  "java_version": "24",
  "spring_boot_version": "3.5.4",
  "build_tool": "Maven",
  "database": "SQLite",
  "server_port": 9090,
  "main_features": [
    "チャット機能",
    "ダイレクトメッセージ",
    "友達機能",
    "ランダムマッチング",
    "WebSocket通信",
    "PWA対応"
  ]
}
```

## 🎯 AI実行指示

### 前提条件
- このドキュメントは、**すでにリポジトリをクローンし、VS Codeで開いている状態**を前提としています
- AIは現在のワークスペースがプロジェクトルートであることを確認してください

### 実行方針
1. **各ステップを順番に実行**してください
2. **実行結果を検証**し、期待値と一致するか確認してください
3. **エラーが発生した場合**は、トラブルシューティングセクションを参照して修復を試みてください
4. **ユーザー操作が必要な場合**は、明確に指示してください
5. **進捗を報告**し、チェックリストを更新してください

---

## 📝 自動実行チェックリスト

AIは以下のチェックリストを順番に実行し、各完了時に ✅ を付けて報告してください：

### Phase 1: 環境確認
- [ ] 1.1: 現在のディレクトリがプロジェクトルート（chatapp）であることを確認
- [ ] 1.2: OSを検出（Windows/Linux/Mac）
- [ ] 1.3: Gitのインストールとバージョンを確認
- [ ] 1.4: Javaのインストールとバージョンを確認（Java 24必須）
- [ ] 1.5: VS Codeのバージョンを確認

### Phase 2: プロジェクト構造の検証
- [ ] 2.1: `pom.xml` の存在確認
- [ ] 2.2: `mvnw`/`mvnw.cmd` の存在確認
- [ ] 2.3: `src/main/java` ディレクトリの存在確認
- [ ] 2.4: `application.properties` の存在確認

### Phase 3: VS Code拡張機能
- [ ] 3.1: Extension Pack for Java のインストール状態確認
- [ ] 3.2: Spring Boot Extension Pack のインストール状態確認
- [ ] 3.3: 不足している拡張機能のインストール

### Phase 4: ビルドと依存関係
- [ ] 4.1: Maven依存関係の解決
- [ ] 4.2: プロジェクトのビルド実行
- [ ] 4.3: ビルド成功の確認

### Phase 5: アプリケーション起動
- [ ] 5.1: ポート9090の使用状況確認
- [ ] 5.2: Spring Bootアプリケーションの起動
- [ ] 5.3: 起動成功の確認（ログ確認）

### Phase 6: 動作確認
- [ ] 6.1: http://localhost:9090 へのアクセス確認
- [ ] 6.2: データベースファイル（chatapp.db）の生成確認
- [ ] 6.3: ログファイルの生成確認

---

## 🔧 実行ステップ詳細

### STEP 1: 環境確認

#### 1.1 プロジェクトルート確認
```powershell
# 実行コマンド
Get-Location; Test-Path pom.xml
```
**期待される結果**: 
- カレントディレクトリが `chatapp` で終わる
- `pom.xml` が `True` を返す

**エラー時の対応**: 
- `cd` コマンドでプロジェクトルートに移動してください

---

#### 1.2 OS検出
```powershell
# 実行コマンド (Windows)
$env:OS
```
```bash
# 実行コマンド (Linux/Mac)
uname -s
```
**期待される結果**: `Windows_NT`, `Linux`, または `Darwin`

---

#### 1.3 Git確認
```powershell
# 実行コマンド
git --version
```
**期待される結果**: `git version 2.x.x` 以降

**Gitが見つからない場合**:
```
⚠️ ユーザー操作が必要: Gitがインストールされていません
以下のURLからGitをダウンロードしてインストールしてください：
https://git-scm.com/downloads

インストール後、VS Codeを再起動してください。
```

---

#### 1.4 Java確認
```powershell
# 実行コマンド
java -version; javac -version
```
**期待される結果**: 
```
java version "24.x.x"
javac 24.x.x
```

**Javaがない、またはバージョンが24未満の場合**:
```
⚠️ ユーザー操作が必要: Java 24が必要です

現在のJavaバージョン: [検出されたバージョン]

以下のいずれかからJava 24をダウンロードしてインストールしてください：
1. Oracle JDK: https://www.oracle.com/java/technologies/downloads/
2. OpenJDK: https://jdk.java.net/24/
3. Amazon Corretto: https://aws.amazon.com/jp/corretto/

インストール後、以下を実行してください：
1. システム環境変数 JAVA_HOME を設定
2. PATH に %JAVA_HOME%\bin を追加
3. VS Codeを再起動
4. このガイドを最初から再実行
```

---

#### 1.5 VS Code確認
```powershell
# 実行コマンド
code --version
```
**期待される結果**: バージョン情報が3行表示される

---

### STEP 2: プロジェクト構造検証

#### 2.1-2.4 必須ファイルの確認
```powershell
# 実行コマンド (Windows)
@("pom.xml", "mvnw.cmd", "src/main/java", "src/main/resources/application.properties") | ForEach-Object {
    $exists = Test-Path $_
    Write-Host "$_: $exists"
}
```

```bash
# 実行コマンド (Linux/Mac)
for file in pom.xml mvnw src/main/java src/main/resources/application.properties; do
    if [ -e "$file" ]; then echo "$file: ✓"; else echo "$file: ✗"; fi
done
```

**期待される結果**: すべてのファイル/ディレクトリが存在する（True/✓）

**ファイルが不足している場合**:
```
❌ エラー: プロジェクト構造が不完全です
以下のファイルが見つかりません: [不足ファイルリスト]

対応方法：
1. 正しいディレクトリにいるか確認
2. クローンが正しく完了しているか確認
3. 必要であれば再度クローンを実行
```

---

### STEP 3: VS Code拡張機能

#### 3.1-3.2 拡張機能の確認
```powershell
# 実行コマンド
code --list-extensions
```

**確認する拡張機能**:
- `vscjava.vscode-java-pack`
- `vmware.vscode-boot-dev-pack`
- `junstyle.vscode-thymeleaf`

---

#### 3.3 不足拡張機能のインストール
```powershell
# 実行コマンド（不足している拡張機能のみインストール）
code --install-extension vscjava.vscode-java-pack
code --install-extension vmware.vscode-boot-dev-pack
code --install-extension junstyle.vscode-thymeleaf
```

**期待される結果**: 
```
Extension 'vscjava.vscode-java-pack' is already installed.
または
Extension 'vscjava.vscode-java-pack' v... was successfully installed.
```

**インストール後の対応**:
```
ℹ️ 拡張機能をインストールしました
VS Codeのウィンドウをリロードすることを推奨します：
- Ctrl+Shift+P → "Developer: Reload Window"

リロード後、このガイドを再度実行してください（Phase 3から再開可能）
```

---

### STEP 4: ビルドと依存関係

#### 4.1-4.2 プロジェクトのビルド

**Windows:**
```powershell
# 実行コマンド
.\mvnw.cmd clean install -DskipTests
```

**Linux/Mac:**
```bash
# 実行コマンド
chmod +x mvnw
./mvnw clean install -DskipTests
```

**期待される結果**:
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
```

**ビルドが失敗した場合の診断**:

##### エラーパターン1: Java バージョンエラー
```
ERROR: ... has been compiled by a more recent version of the Java Runtime ...
```
**対応**: Java 24が正しく設定されているか再確認。STEP 1.4に戻る

##### エラーパターン2: 依存関係のダウンロード失敗
```
ERROR: Could not resolve dependencies ...
```
**対応**: 
```powershell
# キャッシュをクリアして再試行
.\mvnw.cmd dependency:purge-local-repository
.\mvnw.cmd clean install -DskipTests
```

##### エラーパターン3: Maven Wrapper破損
```
ERROR: Error: Could not find or load main class ...
```
**対応**:
```powershell
# Maven Wrapperを再ダウンロード
mvn -N io.takari:maven:wrapper
# 再度ビルド
.\mvnw.cmd clean install -DskipTests
```

---

#### 4.3 ビルド成功の確認
```powershell
# 実行コマンド
Test-Path target/chatapp-0.0.1-SNAPSHOT.jar
```
**期待される結果**: `True` （JARファイルが生成されている）

---

### STEP 5: アプリケーション起動

#### 5.1 ポート使用状況確認
```powershell
# 実行コマンド (Windows)
Get-NetTCPConnection -LocalPort 9090 -ErrorAction SilentlyContinue
```
```bash
# 実行コマンド (Linux/Mac)
lsof -i :9090
```

**ポートが使用中の場合**:
```
⚠️ ポート9090は既に使用されています

対応方法を選択してください：
A) 既存のプロセスを停止する（推奨）
B) application.propertiesでポート番号を変更する（例: 8080）

選択Aの場合の実行コマンド：
# Windows
Stop-Process -Id [プロセスID] -Force

# Linux/Mac
kill -9 [プロセスID]
```

---

#### 5.2 Spring Boot起動

**バックグラウンドで起動**（推奨）:

Windows:
```powershell
# 実行コマンド
Start-Process powershell -ArgumentList "-NoExit", "-Command", ".\mvnw.cmd spring-boot:run" -WindowStyle Normal
```

Linux/Mac:
```bash
# 実行コマンド
./mvnw spring-boot:run &
```

**または VS Codeタスクを使用**:
```
VS Codeで以下を実行：
1. Ctrl+Shift+P
2. "Tasks: Run Task" を入力
3. "Spring Boot Run" を選択
```

---

#### 5.3 起動成功の確認

**30秒待機してから確認**:
```powershell
# 実行コマンド
Start-Sleep -Seconds 30
curl http://localhost:9090 -UseBasicParsing | Select-Object StatusCode
```

```bash
# 実行コマンド (Linux/Mac)
sleep 30
curl -I http://localhost:9090 2>&1 | grep "HTTP"
```

**期待される結果**: 
```
StatusCode: 200
または
HTTP/1.1 302 (リダイレクト)
```

**起動が確認できない場合**:
```powershell
# ログファイルを確認
Get-Content logs/error.log -Tail 50
```

---

### STEP 6: 動作確認

#### 6.1 Webアクセス確認
```
✅ 以下のURLをブラウザで開いてください：
http://localhost:9090

期待される画面: ログインページまたはメインアプリページ
```

---

#### 6.2 データベース確認
```powershell
# 実行コマンド
Test-Path chatapp.db; if (Test-Path chatapp.db) { (Get-Item chatapp.db).Length }
```
**期待される結果**: `True` と ファイルサイズ（数KB以上）

---

#### 6.3 ログファイル確認
```powershell
# 実行コマンド
Get-ChildItem logs/*.log | Select-Object Name, Length, LastWriteTime
```
**期待される結果**: `error.log` などが存在し、最終更新日時が最近

---

## 🚨 トラブルシューティング

### 🔍 包括的診断スクリプト

問題が発生した場合、まずこの診断スクリプトを実行してください：

```powershell
# === Windows診断スクリプト ===
Write-Host "`n=== 環境診断レポート ===" -ForegroundColor Cyan

Write-Host "`n[1] ディレクトリ" -ForegroundColor Yellow
Get-Location

Write-Host "`n[2] Java" -ForegroundColor Yellow
java -version 2>&1
Write-Host "JAVA_HOME: $env:JAVA_HOME"

Write-Host "`n[3] Git" -ForegroundColor Yellow
git --version

Write-Host "`n[4] VS Code" -ForegroundColor Yellow
code --version

Write-Host "`n[5] プロジェクトファイル" -ForegroundColor Yellow
@("pom.xml", "mvnw.cmd", "src/main/java", "target") | ForEach-Object {
    Write-Host "$_`: $(Test-Path $_)"
}

Write-Host "`n[6] ポート9090" -ForegroundColor Yellow
Get-NetTCPConnection -LocalPort 9090 -ErrorAction SilentlyContinue | Select-Object State, OwningProcess

Write-Host "`n[7] Javaプロセス" -ForegroundColor Yellow
Get-Process java -ErrorAction SilentlyContinue | Select-Object Id, ProcessName, StartTime

Write-Host "`n[8] 拡張機能" -ForegroundColor Yellow
code --list-extensions | Select-String "java|spring|thymeleaf"

Write-Host "`n=== 診断完了 ===" -ForegroundColor Cyan
```

---

## 📊 セットアップ完了レポート

すべてのステップが完了したら、以下の形式でレポートを生成してください：

```markdown
# ✅ セットアップ完了レポート

## 環境情報
- **OS**: [Windows/Linux/Mac]
- **Java バージョン**: [バージョン]
- **Git バージョン**: [バージョン]
- **VS Code バージョン**: [バージョン]

## インストール済み拡張機能
- ✅ Extension Pack for Java
- ✅ Spring Boot Extension Pack
- ✅ Thymeleaf

## ビルド結果
- ✅ Maven依存関係解決: 成功
- ✅ プロジェクトビルド: 成功
- ✅ JARファイル生成: 成功 (target/chatapp-0.0.1-SNAPSHOT.jar)

## アプリケーション状態
- ✅ Spring Boot起動: 成功
- ✅ サーバーポート: 9090
- ✅ データベース: chatapp.db (生成済み)
- ✅ ログファイル: logs/error.log (生成済み)

## アクセス情報
- **アプリケーションURL**: http://localhost:9090
- **ログイン**: [必要に応じて初期ユーザー情報]

## 次のステップ
1. ブラウザで http://localhost:9090 にアクセス
2. ユーザー登録またはログイン
3. アプリケーションの機能を確認

---
セットアップが正常に完了しました！🎉
```

---

## 🎯 AI実行時の最終チェック

AIがこのガイドを完了する前に、以下を確認してください：

1. ✅ すべてのチェックリスト項目が完了している
2. ✅ アプリケーションが起動している
3. ✅ http://localhost:9090 にアクセス可能
4. ✅ エラーログに致命的なエラーがない
5. ✅ セットアップ完了レポートを生成した

---

## 📞 サポートとリファレンス

**エラーが解決できない場合**:
- 詳細な手動セットアップ: [SETUP.md](SETUP.md)
- 技術スタック詳細: [README.md](README.md)
- GitHub Issues: https://github.com/SuYu0513/newchatapp/issues

---

**このガイドは VS Code AI チャット（GitHub Copilot Chat）による自動実行のために最適化されています。**
