/**
 * CSRF対応ヘルパー
 * CookieCsrfTokenRepository使用時のCSRFトークン処理
 */
(function() {
    'use strict';

    // CookieからCSRFトークンを取得
    function getCsrfToken() {
        const name = 'XSRF-TOKEN=';
        const decodedCookie = decodeURIComponent(document.cookie);
        const cookies = decodedCookie.split(';');
        for (let i = 0; i < cookies.length; i++) {
            let cookie = cookies[i].trim();
            if (cookie.indexOf(name) === 0) {
                return cookie.substring(name.length);
            }
        }
        return null;
    }

    // オリジナルのfetchを先に保存（無限再帰防止）
    const originalFetch = window.fetch.bind(window);

    // fetchのラッパー関数（CSRF対応）
    function csrfFetch(url, options = {}) {
        const csrfToken = getCsrfToken();

        // デフォルトオプション
        const defaultOptions = {
            credentials: 'same-origin',
            headers: {}
        };

        // オプションをマージ
        const mergedOptions = { ...defaultOptions, ...options };
        mergedOptions.headers = { ...defaultOptions.headers, ...options.headers };

        // CSRFトークンをヘッダーに追加（GET以外のメソッドの場合）
        if (csrfToken && mergedOptions.method && mergedOptions.method.toUpperCase() !== 'GET') {
            mergedOptions.headers['X-XSRF-TOKEN'] = csrfToken;
        }

        // オリジナルのfetchを呼ぶ（window.fetchを呼ぶと自分自身になり無限再帰になるため）
        return originalFetch(url, mergedOptions);
    }

    // XMLHttpRequestにCSRFトークンを自動追加
    const originalXHROpen = XMLHttpRequest.prototype.open;
    const originalXHRSend = XMLHttpRequest.prototype.send;

    XMLHttpRequest.prototype.open = function(method, url) {
        this._csrfMethod = method;
        return originalXHROpen.apply(this, arguments);
    };

    XMLHttpRequest.prototype.send = function(body) {
        const csrfToken = getCsrfToken();
        if (csrfToken && this._csrfMethod && this._csrfMethod.toUpperCase() !== 'GET') {
            this.setRequestHeader('X-XSRF-TOKEN', csrfToken);
        }
        return originalXHRSend.apply(this, arguments);
    };

    // グローバルに公開
    window.csrfHelper = {
        getToken: getCsrfToken,
        fetch: csrfFetch
    };

    // オリジナルのfetchをCSRF対応版に置き換え
    // 全てのfetch呼び出しにCSRFトークンを自動追加
    window.originalFetch = originalFetch;
    window.fetch = csrfFetch;

    console.log('CSRF Helper initialized');

})();
