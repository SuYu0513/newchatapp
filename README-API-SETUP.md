# API設定ガイド

このアプリケーションを動かすには、以下のAPIキーが必要です。

## 必要なAPIキー

### 1. Gemini API Key（AIチャット機能）
- 取得先: https://makersuite.google.com/app/apikey
- 無料枠: あり

### 2. Leonardo AI API Key（アバター生成機能）
- 取得先: https://leonardo.ai
- 無料枠: あり（制限付き）

## 設定方法

### 方法1: ローカル設定ファイル（推奨）

1. `src/main/resources/application-local.properties`を作成
2. 以下の内容を記述：

```properties
gemini.api.key=YOUR_GEMINI_API_KEY
leonardo.api.key=YOUR_LEONARDO_API_KEY
```

3. アプリケーション起動時に`--spring.profiles.active=local`を指定

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 方法2: 環境変数

以下の環境変数を設定：

**Windows (PowerShell):**
```powershell
$env:GEMINI_API_KEY="YOUR_GEMINI_API_KEY"
$env:LEONARDO_API_KEY="YOUR_LEONARDO_API_KEY"
```

**Mac/Linux:**
```bash
export GEMINI_API_KEY="YOUR_GEMINI_API_KEY"
export LEONARDO_API_KEY="YOUR_LEONARDO_API_KEY"
```

## 注意事項

⚠️ **絶対にAPIキーをGitにコミットしないでください！**

- `application-local.properties`は`.gitignore`に追加済み
- APIキーは環境変数または`application-local.properties`で管理
- `.env.example`をコピーして使用可能
