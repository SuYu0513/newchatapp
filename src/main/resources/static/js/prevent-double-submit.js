/**
 * 二重送信防止 & ローディング表示
 * フォーム送信時にボタンを無効化し、ローディングオーバーレイを表示
 */
(function() {
    'use strict';

    // ローディングオーバーレイを作成
    function createLoadingOverlay() {
        if (document.getElementById('loading-overlay')) return;

        const overlay = document.createElement('div');
        overlay.id = 'loading-overlay';
        overlay.innerHTML = `
            <div class="loading-content">
                <div class="loading-spinner"></div>
                <p class="loading-text">処理中...</p>
            </div>
        `;
        document.body.appendChild(overlay);
    }

    // ローディングを表示
    function showLoading(message) {
        const overlay = document.getElementById('loading-overlay');
        if (overlay) {
            const text = overlay.querySelector('.loading-text');
            if (text && message) {
                text.textContent = message;
            }
            overlay.classList.add('active');
        }
    }

    // ローディングを非表示
    function hideLoading() {
        const overlay = document.getElementById('loading-overlay');
        if (overlay) {
            overlay.classList.remove('active');
        }
    }

    // フォームに二重送信防止を適用
    function preventDoubleSubmit(form) {
        let isSubmitting = false;

        form.addEventListener('submit', function(e) {
            // 既に送信中なら阻止
            if (isSubmitting) {
                e.preventDefault();
                e.stopPropagation();
                return false;
            }

            // バリデーションチェック（HTML5バリデーション）
            if (!form.checkValidity()) {
                return true; // ブラウザのバリデーションに任せる
            }

            isSubmitting = true;

            // 送信ボタンを無効化
            const submitBtn = form.querySelector('button[type="submit"], input[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.classList.add('btn-loading');

                // ボタンの元のテキストを保存
                const originalContent = submitBtn.innerHTML;
                submitBtn.setAttribute('data-original-content', originalContent);

                // ローディング表示に変更
                submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>処理中...';
            }

            // オーバーレイ表示
            showLoading('処理中...');

            // 5秒後に自動リセット（エラー時のフォールバック）
            setTimeout(function() {
                if (isSubmitting) {
                    resetForm(form, submitBtn);
                    isSubmitting = false;
                }
            }, 10000);

            return true;
        });
    }

    // フォームをリセット
    function resetForm(form, submitBtn) {
        hideLoading();
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.classList.remove('btn-loading');
            const originalContent = submitBtn.getAttribute('data-original-content');
            if (originalContent) {
                submitBtn.innerHTML = originalContent;
            }
        }
    }

    // リンククリックにもローディングを適用（オプション）
    function addLoadingToLinks() {
        document.querySelectorAll('a[data-loading="true"]').forEach(function(link) {
            link.addEventListener('click', function() {
                showLoading('読み込み中...');
            });
        });
    }

    // DOMContentLoaded時に初期化
    document.addEventListener('DOMContentLoaded', function() {
        createLoadingOverlay();

        // すべてのフォームに適用
        document.querySelectorAll('form').forEach(function(form) {
            // data-no-prevent属性がある場合はスキップ
            if (!form.hasAttribute('data-no-prevent')) {
                preventDoubleSubmit(form);
            }
        });

        addLoadingToLinks();
    });

    // ページ遷移時にローディングを非表示（戻るボタン対策）
    window.addEventListener('pageshow', function(event) {
        if (event.persisted) {
            hideLoading();
            // ボタンもリセット
            document.querySelectorAll('button[type="submit"], input[type="submit"]').forEach(function(btn) {
                btn.disabled = false;
                btn.classList.remove('btn-loading');
                const originalContent = btn.getAttribute('data-original-content');
                if (originalContent) {
                    btn.innerHTML = originalContent;
                }
            });
        }
    });

    // グローバルに公開（必要に応じて使用）
    window.loadingOverlay = {
        show: showLoading,
        hide: hideLoading
    };
})();
