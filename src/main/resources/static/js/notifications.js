/**
 * チャットアプリ - リアルタイム通知システム
 * ブラウザ通知、音声通知、視覚的フィードバックを管理
 */

class ChatNotificationManager {
    constructor() {
        this.notificationPermission = 'default';
        this.isNotificationEnabled = true;
        this.isSoundEnabled = true;
        this.unreadCounts = new Map(); // roomId -> unreadCount
        this.lastNotificationTime = 0;
        this.notificationThrottle = 2000; // 2秒間隔で制限
        this.sounds = {};
        
        this.init();
    }

    /**
     * 通知システムの初期化
     */
    async init() {
        // ブラウザ通知権限の確認
        await this.checkNotificationPermission();
        
        // 音声ファイルの準備
        this.prepareSounds();
        
        // 設定の読み込み
        this.loadSettings();
        
        // UIの初期化（DOMの準備を待つ）
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.initializeUI());
        } else {
            // DOMが既に読み込み済みの場合は少し待ってから初期化
            setTimeout(() => this.initializeUI(), 100);
        }
        
        console.log('📢 通知システムが初期化されました');
    }

    /**
     * ブラウザ通知権限の確認と取得
     */
    async checkNotificationPermission() {
        if (!('Notification' in window)) {
            console.warn('このブラウザは通知をサポートしていません');
            return false;
        }

        this.notificationPermission = Notification.permission;
        
        if (this.notificationPermission === 'default') {
            this.notificationPermission = await Notification.requestPermission();
        }
        
        if (this.notificationPermission === 'granted') {
            console.log('✅ ブラウザ通知が許可されました');
            return true;
        } else {
            console.warn('❌ ブラウザ通知が拒否されました');
            return false;
        }
    }

    /**
     * 音声ファイルの準備
     */
    prepareSounds() {
        // 通知音のデータURL（短いビープ音）
        const messageSound = 'data:audio/wav;base64,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';
        
        this.sounds = {
            message: new Audio(messageSound),
            join: new Audio(messageSound),
            leave: new Audio(messageSound)
        };
        
        // 音量を設定
        Object.values(this.sounds).forEach(sound => {
            sound.volume = 0.3;
        });
    }

    /**
     * 設定の読み込み
     */
    loadSettings() {
        const settings = localStorage.getItem('chatNotificationSettings');
        if (settings) {
            const parsed = JSON.parse(settings);
            this.isNotificationEnabled = parsed.notifications !== false;
            this.isSoundEnabled = parsed.sounds !== false;
        }
    }

    /**
     * 設定の保存
     */
    saveSettings() {
        const settings = {
            notifications: this.isNotificationEnabled,
            sounds: this.isSoundEnabled
        };
        localStorage.setItem('chatNotificationSettings', JSON.stringify(settings));
    }

    /**
     * 新しいメッセージの通知
     */
    notifyNewMessage(message, roomName, currentRoomId) {
        const messageRoomId = message.chatRoomId || message.roomId;
        
        // 現在のルームのメッセージは通知しない
        if (messageRoomId === currentRoomId) {
            return;
        }
        
        // 自分のメッセージは通知しない
        if (message.senderUsername === window.currentUser) {
            return;
        }
        
        // スロットリング（連続通知を防ぐ）
        const now = Date.now();
        if (now - this.lastNotificationTime < this.notificationThrottle) {
            return;
        }
        this.lastNotificationTime = now;
        
        // 未読数を更新
        this.incrementUnreadCount(messageRoomId, roomName);
        
        // ブラウザ通知
        if (this.isNotificationEnabled && this.notificationPermission === 'granted') {
            this.showBrowserNotification(message, roomName);
        }
        
        // 音声通知
        if (this.isSoundEnabled) {
            this.playSound('message');
        }
        
        // タブタイトル更新
        this.updateTabTitle();
    }

    /**
     * ユーザー参加の通知
     */
    notifyUserJoined(username, roomName, currentRoomId, messageRoomId) {
        if (messageRoomId === currentRoomId) {
            return; // 現在のルームは通知しない
        }
        
        if (this.isSoundEnabled) {
            this.playSound('join');
        }
        
        // 軽い視覚的通知のみ
        this.showToast(`${username}さんが${roomName}に参加しました`, 'info');
    }

    /**
     * ユーザー退出の通知
     */
    notifyUserLeft(username, roomName, currentRoomId, messageRoomId) {
        if (messageRoomId === currentRoomId) {
            return; // 現在のルームは通知しない
        }
        
        if (this.isSoundEnabled) {
            this.playSound('leave');
        }
    }

    /**
     * ブラウザ通知の表示
     */
    showBrowserNotification(message, roomName) {
        const title = `💬 ${roomName}`;
        const options = {
            body: `${message.senderUsername}: ${message.content}`,
            icon: '/favicon.ico',
            badge: '/favicon.ico',
            tag: `chat-${message.chatRoomId || message.roomId}`,
            renotify: true,
            requireInteraction: false,
            data: {
                roomId: message.chatRoomId || message.roomId,
                roomName: roomName
            }
        };
        
        const notification = new Notification(title, options);
        
        // 通知クリック時にルーム切り替え
        notification.onclick = (event) => {
            event.preventDefault();
            window.focus();
            
            const roomId = event.target.data.roomId;
            if (roomId && window.switchRoom) {
                window.switchRoom(roomId, event.target.data.roomName);
            }
            
            notification.close();
        };
        
        // 自動で閉じる
        setTimeout(() => {
            notification.close();
        }, 5000);
    }

    /**
     * 音声再生
     */
    playSound(type) {
        if (this.sounds[type]) {
            this.sounds[type].currentTime = 0;
            this.sounds[type].play().catch(e => {
                console.warn('音声再生に失敗:', e);
            });
        }
    }

    /**
     * 未読数の増加
     */
    incrementUnreadCount(roomId, roomName) {
        const current = this.unreadCounts.get(roomId) || 0;
        this.unreadCounts.set(roomId, current + 1);
        
        // UIの更新
        this.updateUnreadBadge(roomId, current + 1);
        this.updateTabTitle();
    }

    /**
     * 未読数のクリア
     */
    clearUnreadCount(roomId) {
        this.unreadCounts.delete(roomId);
        this.updateUnreadBadge(roomId, 0);
        this.updateTabTitle();
    }

    /**
     * 未読バッジの更新
     */
    updateUnreadBadge(roomId, count) {
        const roomElement = document.querySelector(`[data-room-id="${roomId}"]`);
        if (!roomElement) return;
        
        // 既存のバッジを削除
        const existingBadge = roomElement.querySelector('.unread-badge');
        if (existingBadge) {
            existingBadge.remove();
        }
        
        // 新しいバッジを追加
        if (count > 0) {
            const badge = document.createElement('span');
            badge.className = 'badge bg-danger unread-badge ms-1';
            badge.textContent = count > 99 ? '99+' : count;
            badge.style.fontSize = '0.75rem';
            badge.style.animation = 'bounce 0.5s ease-in-out';
            
            const nameElement = roomElement.querySelector('.fw-bold');
            if (nameElement) {
                nameElement.appendChild(badge);
            }
        }
    }

    /**
     * タブタイトルの更新
     */
    updateTabTitle() {
        const totalUnread = Array.from(this.unreadCounts.values()).reduce((sum, count) => sum + count, 0);
        const originalTitle = 'チャット - チャットアプリ';
        
        if (totalUnread > 0) {
            document.title = `(${totalUnread}) ${originalTitle}`;
        } else {
            document.title = originalTitle;
        }
    }

    /**
     * トースト通知の表示
     */
    showToast(message, type = 'info') {
        // トースト要素を作成
        const toast = document.createElement('div');
        toast.className = `toast align-items-center text-white bg-${type === 'info' ? 'primary' : type} border-0`;
        toast.setAttribute('role', 'alert');
        toast.style.position = 'fixed';
        toast.style.top = '20px';
        toast.style.right = '20px';
        toast.style.zIndex = '9999';
        
        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">
                    <i class="fas fa-info-circle me-2"></i>${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" onclick="this.parentElement.parentElement.remove()"></button>
            </div>
        `;
        
        document.body.appendChild(toast);
        
        // Bootstrap Toastを初期化
        const bsToast = new bootstrap.Toast(toast, {
            autohide: true,
            delay: 3000
        });
        bsToast.show();
        
        // 自動削除
        setTimeout(() => {
            if (toast.parentNode) {
                toast.remove();
            }
        }, 4000);
    }

    /**
     * 通知設定UIの初期化
     */
    initializeUI() {
        try {
            // DOMが存在するかチェック
            if (!document.body) {
                console.warn('DOM がまだ準備されていません');
                setTimeout(() => this.initializeUI(), 100);
                return;
            }
            
            // 設定ボタンをナビゲーションバーに追加
            this.addNotificationSettingsButton();
        } catch (error) {
            console.error('UIの初期化でエラー:', error);
        }
    }

    /**
     * 設定ボタンの追加
     */
    addNotificationSettingsButton() {
        try {
            const navbar = document.querySelector('.navbar-nav');
            if (!navbar) {
                console.warn('navbar-nav が見つかりません');
                return;
            }
            
            const settingsItem = document.createElement('div');
            settingsItem.className = 'nav-item dropdown';
            settingsItem.id = 'notificationSettings';
            settingsItem.innerHTML = `
                <a class="nav-link dropdown-toggle d-flex align-items-center" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                    <i class="fas fa-bell me-1"></i><span class="d-none d-md-inline">通知設定</span>
                </a>
                <ul class="dropdown-menu">
                    <li>
                        <div class="form-check px-3 py-2">
                            <input class="form-check-input" type="checkbox" id="notificationToggle" ${this.isNotificationEnabled ? 'checked' : ''}>
                            <label class="form-check-label" for="notificationToggle">
                                <i class="fas fa-bell me-1"></i>ブラウザ通知
                            </label>
                        </div>
                    </li>
                    <li>
                        <div class="form-check px-3 py-2">
                            <input class="form-check-input" type="checkbox" id="soundToggle" ${this.isSoundEnabled ? 'checked' : ''}>
                            <label class="form-check-label" for="soundToggle">
                                <i class="fas fa-volume-up me-1"></i>音声通知
                            </label>
                        </div>
                    </li>
                    <li><hr class="dropdown-divider"></li>
                    <li>
                        <a class="dropdown-item" href="#" id="testNotification">
                            <i class="fas fa-test-tube me-1"></i>通知テスト
                        </a>
                    </li>
                </ul>
            `;
            
            // 既存の設定ボタンを削除（重複防止）
            const existingSettings = navbar.querySelector('#notificationSettings');
            if (existingSettings) {
                existingSettings.remove();
            }
            
            // 最も安全な方法：末尾に追加
            navbar.appendChild(settingsItem);
            
            console.log('✅ 通知設定ボタンを追加しました');
            
            // イベントリスナーを少し遅延して追加（DOM安定化のため）
            setTimeout(() => {
                this.attachSettingsEventListeners();
            }, 50);
        } catch (error) {
            console.error('通知設定ボタンの追加でエラー:', error);
        }
    }

    /**
     * 設定のイベントリスナー
     */
    attachSettingsEventListeners() {
        try {
            const notificationToggle = document.getElementById('notificationToggle');
            const soundToggle = document.getElementById('soundToggle');
            const testButton = document.getElementById('testNotification');
            
            if (notificationToggle) {
                notificationToggle.addEventListener('change', (e) => {
                    this.isNotificationEnabled = e.target.checked;
                    this.saveSettings();
                    
                    if (this.isNotificationEnabled && this.notificationPermission !== 'granted') {
                        this.checkNotificationPermission();
                    }
                });
            } else {
                console.warn('notificationToggle が見つかりません');
            }
            
            if (soundToggle) {
                soundToggle.addEventListener('change', (e) => {
                    this.isSoundEnabled = e.target.checked;
                this.saveSettings();
            });
            } else {
                console.warn('soundToggle が見つかりません');
            }
            
            if (testButton) {
                testButton.addEventListener('click', (e) => {
                    e.preventDefault();
                    this.testNotification();
                });
            } else {
                console.warn('testButton が見つかりません');
            }
        } catch (error) {
            console.error('イベントリスナーの設定でエラー:', error);
        }
    }

    /**
     * 通知テスト
     */
    testNotification() {
        const testMessage = {
            senderUsername: 'テストユーザー',
            content: 'これはテスト通知です！',
            chatRoomId: 999
        };
        
        if (this.isNotificationEnabled) {
            this.showBrowserNotification(testMessage, 'テストルーム');
        }
        
        if (this.isSoundEnabled) {
            this.playSound('message');
        }
        
        this.showToast('テスト通知を送信しました！', 'success');
    }

    /**
     * 通知マネージャーの取得
     */
    getTotalUnreadCount() {
        return Array.from(this.unreadCounts.values()).reduce((sum, count) => sum + count, 0);
    }

    /**
     * 特定ルームの未読数取得
     */
    getUnreadCount(roomId) {
        return this.unreadCounts.get(roomId) || 0;
    }
}

// グローバルに通知マネージャーを設定
// DOMの準備を待ってからインスタンス化
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.notificationManager = new ChatNotificationManager();
    });
} else {
    window.notificationManager = new ChatNotificationManager();
}
