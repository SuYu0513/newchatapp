-- 論理名設定確認用SQL
-- 設定後に実行して論理名が正しく設定されているか確認

-- 1. テーブル論理名一覧を表示
SELECT '=== テーブル論理名一覧 ===' as '';
SELECT * FROM v_table_info;

-- 2. usersテーブルのカラム論理名を表示
SELECT '=== usersテーブル カラム論理名 ===' as '';
SELECT * FROM v_column_info WHERE テーブル名 = 'users';

-- 3. messagesテーブルのカラム論理名を表示
SELECT '=== messagesテーブル カラム論理名 ===' as '';
SELECT * FROM v_column_info WHERE テーブル名 = 'messages';

-- 4. chat_roomsテーブルのカラム論理名を表示
SELECT '=== chat_roomsテーブル カラム論理名 ===' as '';
SELECT * FROM v_column_info WHERE テーブル名 = 'chat_rooms';

-- 5. 特定のカラムの論理名を検索
SELECT '=== 論理名で検索例（IDカラム） ===' as '';
SELECT 
    table_name as 'テーブル名',
    column_name as 'カラム名', 
    logical_name as '論理名',
    description as '説明'
FROM column_comments 
WHERE logical_name LIKE '%ID%'
ORDER BY table_name, column_name;

-- 6. データベース設計書用クエリ
SELECT '=== データベース設計書 ===' as '';
SELECT 
    tc.logical_name as 'テーブル論理名',
    tc.table_name as '物理テーブル名',
    tc.description as 'テーブル説明'
FROM table_comments tc
ORDER BY tc.table_name;