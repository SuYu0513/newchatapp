# RenderでPostgreSQL DBをセットアップする手順

## 📝 概要
RenderでPostgreSQLデータベースを作成し、アプリに接続してデータを永続化します。

---

## 🎯 ステップ1: PostgreSQLデータベースを作成

### 1. Renderダッシュボードを開く
- https://dashboard.render.com にアクセス

### 2. 新しいPostgreSQLを作成
1. **「New +」** ボタンをクリック
2. **「PostgreSQL」** を選択

### 3. データベース設定
- **Name**: `chatapp-db` (任意の名前)
- **Database**: `chatapp` (自動入力でOK)
- **User**: `chatapp` (自動入力でOK)
- **Region**: `Singapore (Southeast Asia)` (アプリと同じリージョンを推奨)
- **PostgreSQL Version**: `17` (最新版でOK)
- **Instance Type**: `Free` を選択

### 4. 作成を実行
- **「Create Database」** をクリック
- 作成に1〜2分かかります

---

## 🎯 ステップ2: データベース情報を確認

### 1. データベースのダッシュボードを開く
- 作成された `chatapp-db` をクリック

### 2. 接続情報を確認
画面上部に表示される以下の情報をメモ:
- **Internal Database URL** (Renderアプリ内部から接続する場合)
- **External Database URL** (外部から接続する場合)

例:
```
postgres://chatapp_user:パスワード@dpg-xxxxx.singapore-postgres.render.com/chatapp_db
```

---

## 🎯 ステップ3: Web ServiceにDB接続情報を追加

### 1. Web Serviceのダッシュボードを開く
- 左メニューから **「newchatapp-nidaimeapuri」** (あなたのアプリ) を選択

### 2. Environment変数を追加
1. 左メニューから **「Environment」** をクリック
2. 以下の環境変数を追加:

#### 必須の環境変数

| Key | Value |
|-----|-------|
| `DATABASE_URL` | `postgres://...` (Internal Database URLをコピペ) |
| `DB_DRIVER` | `org.postgresql.Driver` |
| `DB_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` |

**注意:** `DATABASE_URL`は必ず **Internal Database URL** を使用してください！

### 3. 既存の環境変数も確認
以下が設定されているか確認:
- `GEMINI_API_KEY`
- `LEONARDO_API_KEY`

---

## 🎯 ステップ4: アプリを再デプロイ

### 1. Manual Deployを実行
1. Web Serviceのダッシュボードで **「Manual Deploy」** をクリック
2. **「Deploy latest commit」** を選択
3. デプロイが開始されます（5〜7分）

### 2. ログを確認
- デプロイログで以下を確認:
  - PostgreSQLドライバがロードされているか
  - テーブルが自動作成されているか
  - エラーが出ていないか

---

## ✅ 動作確認

### 1. アプリにアクセス
- https://newchatapp-nidaimeapuri.onrender.com

### 2. 新規ユーザー登録
- ユーザーを作成してログイン

### 3. チャットを試す
- メッセージを送信
- AIチャットを試す

### 4. アプリを再起動して確認
1. Renderダッシュボードで **「Manual Deploy」** → **「Clear build cache & deploy」**
2. 再起動後、ログインしてデータが残っているか確認

✅ **データが残っていればPostgreSQL接続成功！**

---

## 🔧 トラブルシューティング

### エラー: `No suitable driver found for jdbc:sqlite`
**原因:** 環境変数が正しく設定されていない

**解決:**
1. Environment変数で `DATABASE_URL` が設定されているか確認
2. `DB_DRIVER=org.postgresql.Driver` が設定されているか確認
3. 設定後、必ず **Manual Deploy** で再デプロイ

### エラー: `Connection refused`
**原因:** データベースURLが間違っている

**解決:**
1. PostgreSQLダッシュボードで **Internal Database URL** をコピー
2. `DATABASE_URL` に正しいURLを設定
3. **External Database URL** ではなく **Internal** を使う

### データベースが見つからない
**原因:** PostgreSQLが作成されていない

**解決:**
1. Renderダッシュボードで左メニューの **PostgreSQL** セクションを確認
2. `chatapp-db` が表示されているか確認
3. なければ「ステップ1」からやり直す

---

## 📊 無料プランの制限

| 項目 | 制限 |
|------|------|
| **ストレージ** | 1GB |
| **有効期限** | 作成から90日間 |
| **接続数** | 最大97接続 |

**注意:** 90日後は有料プラン ($7/月) に移行するか、新しいDBを作成する必要があります。

---

## 🎉 完了！

これでアプリは本番環境でPostgreSQLを使用し、データが永続化されます！

**次のステップ:**
- PWAとしてスマホのホーム画面に追加
- 友達とチャットを楽しむ
- AIと会話する

---

**トラブルがあれば、Renderのログを確認して原因を特定してください！**
