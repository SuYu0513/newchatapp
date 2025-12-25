-- 古いフレンド申請データをクリーンアップ
-- 注意: このスクリプトを実行すると、全てのフレンド関係が削除されます

-- friendshipsテーブルの全データを削除
DELETE FROM friendships;

-- オプション: 特定のユーザー以外のフレンド申請を削除する場合
-- DELETE FROM friendships WHERE requester_id NOT IN (SELECT id FROM users WHERE username IN ('hanako', 'taro'));

-- vacuum実行でデータベースを最適化
VACUUM;

SELECT '全てのフレンド申請を削除しました' as message;
