-- データベース論理名設定用SQL
-- DBeaverのSQLエディタで実行してください

-- SQLiteでは直接COMMENTを追加できないため、
-- カスタムメタデータテーブルを作成して論理名を管理します

-- メタデータテーブルを作成（既存の場合は削除して再作成）
DROP TABLE IF EXISTS table_comments;
DROP TABLE IF EXISTS column_comments;

-- テーブル論理名管理テーブル
CREATE TABLE table_comments (
    table_name TEXT PRIMARY KEY,
    logical_name TEXT NOT NULL,
    description TEXT
);

-- カラム論理名管理テーブル
CREATE TABLE column_comments (
    table_name TEXT NOT NULL,
    column_name TEXT NOT NULL,
    logical_name TEXT NOT NULL,
    description TEXT,
    PRIMARY KEY (table_name, column_name)
);

-- テーブル論理名を登録
INSERT INTO table_comments (table_name, logical_name, description) VALUES
('users', 'ユーザー情報', 'システム利用者の基本情報を管理'),
('messages', 'メッセージ', 'チャットメッセージ情報を管理'),
('chat_rooms', 'チャットルーム', 'チャットルーム情報を管理'),
('user_profiles', 'ユーザープロフィール', 'ユーザーの詳細プロフィール情報'),
('friendships', 'フレンド関係', 'ユーザー間のフレンド関係を管理'),
('friend_requests', 'フレンド申請', 'フレンド申請の状況を管理'),
('notifications', '通知', 'システム通知情報を管理'),
('random_matches', 'ランダムマッチ', 'ランダムマッチング情報を管理'),
('reports', '通報', 'ユーザー通報情報を管理'),
('admin_notifications', '管理者通知', '管理者向け通知情報');

-- usersテーブルのカラム論理名を登録
INSERT INTO column_comments (table_name, column_name, logical_name, description) VALUES
('users', 'id', 'ユーザーID', '主キー、自動採番'),
('users', 'username', 'ログイン名', 'ユーザーがログインに使用する名前'),
('users', 'password', 'パスワード', 'ハッシュ化済みパスワード'),
('users', 'email', 'メールアドレス', 'ユーザーの連絡先メール'),
('users', 'friend_code', 'フレンドコード', '8桁の一意なフレンド識別番号'),
('users', 'created_at', '作成日時', 'アカウント作成日時'),
('users', 'updated_at', '更新日時', '最終更新日時');

-- messagesテーブルのカラム論理名を登録
INSERT INTO column_comments (table_name, column_name, logical_name, description) VALUES
('messages', 'id', 'メッセージID', '主キー、自動採番'),
('messages', 'content', 'メッセージ内容', 'チャットメッセージの本文'),
('messages', 'sent_at', '送信日時', 'メッセージ送信日時'),
('messages', 'user_id', '送信者ID', 'メッセージ送信者のユーザーID'),
('messages', 'chat_room_id', 'チャットルームID', 'メッセージが送信されたチャットルーム');

-- chat_roomsテーブルのカラム論理名を登録
INSERT INTO column_comments (table_name, column_name, logical_name, description) VALUES
('chat_rooms', 'id', 'ルームID', '主キー、自動採番'),
('chat_rooms', 'name', 'ルーム名', 'チャットルームの表示名'),
('chat_rooms', 'type', 'ルームタイプ', 'GROUP（グループ）またはPRIVATE（プライベート）'),
('chat_rooms', 'created_by', '作成者ID', 'ルームを作成したユーザーのID'),
('chat_rooms', 'created_at', '作成日時', 'ルーム作成日時'),
('chat_rooms', 'updated_at', '更新日時', '最終更新日時');

-- user_profilesテーブルのカラム論理名を登録
INSERT INTO column_comments (table_name, column_name, logical_name, description) VALUES
('user_profiles', 'id', 'プロフィールID', '主キー、自動採番'),
('user_profiles', 'user_id', 'ユーザーID', '対応するユーザーのID'),
('user_profiles', 'display_name', '表示名', 'ユーザーが設定する表示名'),
('user_profiles', 'bio', '自己紹介', 'ユーザーの自己紹介文'),
('user_profiles', 'avatar_url', 'アバターURL', 'プロフィール画像のURL'),
('user_profiles', 'status', 'オンライン状態', 'ONLINE, AWAY, BUSY, OFFLINE'),
('user_profiles', 'last_seen', '最終ログイン', '最後にオンラインだった日時'),
('user_profiles', 'created_at', '作成日時', 'プロフィール作成日時'),
('user_profiles', 'updated_at', '更新日時', '最終更新日時');

-- friendshipsテーブルのカラム論理名を登録
INSERT INTO column_comments (table_name, column_name, logical_name, description) VALUES
('friendships', 'id', 'フレンド関係ID', '主キー、自動採番'),
('friendships', 'user_id', 'ユーザーID', 'フレンド関係の一方のユーザー'),
('friendships', 'friend_id', 'フレンドID', 'フレンド関係のもう一方のユーザー'),
('friendships', 'status', 'フレンド状態', 'PENDING, ACCEPTED, BLOCKED'),
('friendships', 'created_at', '作成日時', 'フレンド関係成立日時'),
('friendships', 'updated_at', '更新日時', '最終更新日時');

-- 論理名確認用ビューを作成
CREATE VIEW v_table_info AS
SELECT 
    t.table_name as '物理テーブル名',
    tc.logical_name as 'テーブル論理名',
    tc.description as 'テーブル説明'
FROM (
    SELECT DISTINCT name as table_name 
    FROM sqlite_master 
    WHERE type = 'table' 
    AND name NOT LIKE 'sqlite_%'
    AND name NOT IN ('table_comments', 'column_comments')
) t
LEFT JOIN table_comments tc ON t.table_name = tc.table_name
ORDER BY t.table_name;

-- カラム情報確認用ビューを作成
CREATE VIEW v_column_info AS
SELECT 
    p.name as 'テーブル名',
    tc.logical_name as 'テーブル論理名',
    p.name as '物理カラム名',
    cc.logical_name as 'カラム論理名',
    p.type as 'データ型',
    CASE WHEN p.pk = 1 THEN 'はい' ELSE 'いいえ' END as '主キー',
    CASE WHEN p."notnull" = 1 THEN 'はい' ELSE 'いいえ' END as 'NOT NULL',
    cc.description as '説明'
FROM pragma_table_info('users') p
LEFT JOIN table_comments tc ON 'users' = tc.table_name
LEFT JOIN column_comments cc ON 'users' = cc.table_name AND p.name = cc.column_name

UNION ALL

SELECT 
    p.name as 'テーブル名',
    tc.logical_name as 'テーブル論理名',
    p.name as '物理カラム名',
    cc.logical_name as 'カラム論理名',
    p.type as 'データ型',
    CASE WHEN p.pk = 1 THEN 'はい' ELSE 'いいえ' END as '主キー',
    CASE WHEN p."notnull" = 1 THEN 'はい' ELSE 'いいえ' END as 'NOT NULL',
    cc.description as '説明'
FROM pragma_table_info('messages') p
LEFT JOIN table_comments tc ON 'messages' = tc.table_name
LEFT JOIN column_comments cc ON 'messages' = cc.table_name AND p.name = cc.column_name

UNION ALL

SELECT 
    p.name as 'テーブル名',
    tc.logical_name as 'テーブル論理名',
    p.name as '物理カラム名',
    cc.logical_name as 'カラム論理名',
    p.type as 'データ型',
    CASE WHEN p.pk = 1 THEN 'はい' ELSE 'いいえ' END as '主キー',
    CASE WHEN p."notnull" = 1 THEN 'はい' ELSE 'いいえ' END as 'NOT NULL',
    cc.description as '説明'
FROM pragma_table_info('chat_rooms') p
LEFT JOIN table_comments tc ON 'chat_rooms' = tc.table_name
LEFT JOIN column_comments cc ON 'chat_rooms' = cc.table_name AND p.name = cc.column_name;

-- 確認用クエリ
-- SELECT * FROM v_table_info;
-- SELECT * FROM v_column_info WHERE テーブル名 = 'users';