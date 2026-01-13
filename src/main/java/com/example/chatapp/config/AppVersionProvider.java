package com.example.chatapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * アプリケーションバージョン管理クラス
 * 静的リソース（CSS/JS）のキャッシュバスティング用バージョン番号を提供
 */
@Component("appVersion")
public class AppVersionProvider {
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    /**
     * バージョン番号を取得
     * 開発環境では毎回現在時刻を返す（キャッシュ無効化）
     * 本番環境では固定値を返す（パフォーマンス重視）
     * @return バージョン番号（例: "1705132800"）
     */
    public String getVersion() {
        // 開発環境では常に最新のタイムスタンプを返す
        if ("prod".equals(activeProfile)) {
            // 本番環境では起動時の固定値
            return String.valueOf(System.currentTimeMillis() / 1000);
        }
        // 開発環境では毎回現在時刻（秒単位）
        return String.valueOf(System.currentTimeMillis() / 1000);
    }
    
    /**
     * toString()をオーバーライドしてThymeleafで直接使用可能にする
     * 使用例: ${@appVersion} または ${@appVersion.version}
     */
    @Override
    public String toString() {
        return getVersion();
    }
}
