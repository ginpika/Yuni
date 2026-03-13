const i18n = {
    currentLang: 'zh',
    
    translations: {
        zh: {
            title: 'Yuni AI 助手',
            wsStatus: 'WebSocket 状态',
            wsDisconnected: '未连接',
            wsConnected: '已连接',
            wsConnecting: '连接中...',
            newConversation: '新对话',
            loadMore: '加载更多',
            noSessions: '暂无会话',
            database: '数据库',
            databaseTitle: '数据库管理',
            clear: '清空',
            contextUsage: '上下文用量',
            tokens: 'tokens',
            loadingHistory: '加载历史消息...',
            inputPlaceholder: '输入你的问题...',
            send: '发送',
            permissionRequest: '权限请求',
            aiTerminalRequest: 'AI 请求执行终端命令',
            command: '命令：',
            rememberCommand: '记住此命令（加入白名单）',
            reject: '拒绝',
            allow: '允许',
            apiResponseDetail: 'API 响应详情',
            copy: '复制',
            copied: '已复制',
            close: '关闭',
            clickToViewDetail: '点击查看详情',
            clickToViewRaw: '点击查看原始响应',
            calling: '调用中...',
            completed: '完成',
            success: '成功',
            failed: '失败',
            greeting: '你好！我是 Yuni AI 助手，有什么可以帮助你的吗？',
            noResponse: '无响应',
            errorOccurred: '抱歉，发生了错误：',
            switchSessionFailed: '切换会话失败',
            createSessionFailed: '创建新会话失败',
            wsDisconnectedAlert: 'WebSocket 连接已断开，请刷新页面重试',
            loadSessionFailed: '加载会话列表失败',
            loadHistoryFailed: '加载历史消息失败'
        },
        en: {
            title: 'Yuni AI Assistant',
            wsStatus: 'WebSocket Status',
            wsDisconnected: 'Disconnected',
            wsConnected: 'Connected',
            wsConnecting: 'Connecting...',
            newConversation: 'New Chat',
            loadMore: 'Load More',
            noSessions: 'No sessions',
            database: 'Database',
            databaseTitle: 'Database Management',
            clear: 'Clear',
            contextUsage: 'Context Usage',
            tokens: 'tokens',
            loadingHistory: 'Loading history...',
            inputPlaceholder: 'Type your question...',
            send: 'Send',
            permissionRequest: 'Permission Request',
            aiTerminalRequest: 'AI requests to execute terminal command',
            command: 'Command:',
            rememberCommand: 'Remember this command (add to whitelist)',
            reject: 'Reject',
            allow: 'Allow',
            apiResponseDetail: 'API Response Details',
            copy: 'Copy',
            copied: 'Copied',
            close: 'Close',
            clickToViewDetail: 'Click to view details',
            clickToViewRaw: 'Click to view raw response',
            calling: 'Calling...',
            completed: 'Completed',
            success: 'Success',
            failed: 'Failed',
            greeting: 'Hello! I am Yuni AI Assistant. How can I help you?',
            noResponse: 'No response',
            errorOccurred: 'Sorry, an error occurred: ',
            switchSessionFailed: 'Failed to switch session',
            createSessionFailed: 'Failed to create new session',
            wsDisconnectedAlert: 'WebSocket disconnected, please refresh the page',
            loadSessionFailed: 'Failed to load session list',
            loadHistoryFailed: 'Failed to load history'
        }
    },
    
    t(key) {
        return this.translations[this.currentLang][key] || key;
    },
    
    setLang(lang) {
        if (this.translations[lang]) {
            this.currentLang = lang;
            localStorage.setItem('yuni-lang', lang);
            this.updateAllText();
            return true;
        }
        return false;
    },
    
    getLang() {
        return this.currentLang;
    },
    
    init() {
        const savedLang = localStorage.getItem('yuni-lang');
        if (savedLang && this.translations[savedLang]) {
            this.currentLang = savedLang;
        } else {
            const browserLang = navigator.language.toLowerCase();
            if (browserLang.startsWith('zh')) {
                this.currentLang = 'zh';
            } else {
                this.currentLang = 'en';
            }
        }
        this.updateAllText();
    },
    
    updateAllText() {
        document.querySelectorAll('[data-i18n]').forEach(el => {
            const key = el.getAttribute('data-i18n');
            if (el.tagName === 'INPUT' && el.hasAttribute('placeholder')) {
                el.placeholder = this.t(key);
            } else {
                el.textContent = this.t(key);
            }
        });
        
        document.querySelectorAll('[data-i18n-title]').forEach(el => {
            el.title = this.t(el.getAttribute('data-i18n-title'));
        });
        
        document.title = this.t('title');
        
        if (typeof updateWsStatus === 'function' && typeof currentWsStatus !== 'undefined') {
            updateWsStatus(currentWsStatus);
        }
    }
};

let currentWsStatus = 'disconnected';
