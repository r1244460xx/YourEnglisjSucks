import React, { useState } from 'react';
import { X, Key, Check, Info, ShieldAlert } from 'lucide-react';

export default function SettingsModal({
  isOpen,
  onClose,
  apiKey,
  setApiKey,
  hasBackendApiKey,
  modelName,
  setModelName
}) {
  const [localKey, setLocalKey] = useState(apiKey || '');
  const [savedSuccess, setSavedSuccess] = useState(false);

  if (!isOpen) return null;

  const handleSave = () => {
    setApiKey(localKey.trim());
    localStorage.setItem('yourenglishsucks_gemini_api_key', localKey.trim());
    setSavedSuccess(true);
    setTimeout(() => {
      setSavedSuccess(false);
      onClose();
    }, 800);
  };

  const handleClear = () => {
    setLocalKey('');
    setApiKey('');
    localStorage.removeItem('yourenglishsucks_gemini_api_key');
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-[var(--card)] border border-[var(--border)] rounded-2xl max-w-md w-full p-6 shadow-xl space-y-5 animate-in fade-in zoom-in-95">
        
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-[var(--border)] pb-3">
          <div className="flex items-center gap-2">
            <Key className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
            <h3 className="font-bold text-base text-[var(--foreground)]">服務與模型設定</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-[var(--muted)] hover:text-[var(--foreground)] hover:bg-[var(--background)] transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Backend Status Alert */}
        <div className="p-3.5 rounded-xl bg-[var(--background)] border border-[var(--border)] text-xs space-y-1.5">
          <div className="font-semibold text-[var(--foreground)] flex items-center gap-1.5">
            <Info className="w-4 h-4 text-indigo-500" />
            <span>後端連線狀態：</span>
          </div>
          <div className="text-[var(--muted)] pl-5">
            {hasBackendApiKey ? (
              <span className="text-emerald-600 dark:text-emerald-400 font-medium">
                ✅ 後端環境已配置 GEMINI_API_KEY，已啟用真實 Gemini 大模型！
              </span>
            ) : (
              <span className="text-amber-600 dark:text-amber-400 font-medium leading-relaxed block">
                ⚠️ 後端未偵測到全域 API Key。若未填入金鑰，系統將處於 <strong>Mock 本地模擬模式</strong>（回傳預設的靜態專案延期測試假資料，無法理解真實文本）。請在下方填寫您的 API Key 以啟用真實 AI！
              </span>
            )}
          </div>
        </div>

        {/* Input Gemini API Key */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between">
            <label className="text-xs font-semibold text-[var(--foreground)]">
              自定義 Gemini API Key (可選)
            </label>
            <a
              href="https://aistudio.google.com/app/apikey"
              target="_blank"
              rel="noreferrer"
              className="text-[11px] text-indigo-600 dark:text-indigo-400 hover:underline flex items-center gap-0.5"
            >
              取得免費 Google API Key ↗
            </a>
          </div>
          <input
            type="password"
            value={localKey}
            onChange={(e) => setLocalKey(e.target.value)}
            placeholder="AIzaSy..."
            className="w-full px-3 py-2 rounded-xl bg-[var(--background)] border border-[var(--border)] focus:border-indigo-500 focus:outline-none text-xs text-[var(--foreground)] font-mono"
          />
          <p className="text-[11px] text-[var(--muted)]">
            金鑰僅儲存於本機瀏覽器 LocalStorage 中，每次請求時透過 Header 帶入呼叫 Google 官方 API。
          </p>
        </div>

        {/* Model Selection */}
        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-[var(--foreground)]">
            預設模型 (Default Model)
          </label>
          <select
            value={modelName}
            onChange={(e) => setModelName(e.target.value)}
            className="w-full px-3 py-2 rounded-xl bg-[var(--background)] border border-[var(--border)] focus:border-indigo-500 focus:outline-none text-xs text-[var(--foreground)]"
          >
            <option value="gemini-3.6-flash">Gemini 3.6 Flash (官方推薦、速度與品質兼具，免費額度)</option>
            <option value="gemini-3.5-flash-lite">Gemini 3.5 Flash Lite (輕量極速)</option>
            <option value="gemini-3.1-flash-lite">Gemini 3.1 Flash Lite (輕量穩定)</option>
            <option value="gemini-2.5-flash">Gemini 2.5 Flash (舊版過渡模型)</option>
          </select>
        </div>

        {/* Actions */}
        <div className="flex items-center justify-between pt-3 border-t border-[var(--border)]">
          <button
            onClick={handleClear}
            className="text-xs text-red-500 hover:text-red-600 px-2 py-1 transition"
          >
            清除設定
          </button>

          <div className="flex items-center gap-2">
            <button
              onClick={onClose}
              className="text-xs px-3.5 py-1.5 rounded-xl border border-[var(--border)] hover:bg-[var(--background)] text-[var(--muted)] hover:text-[var(--foreground)] transition"
            >
              取消
            </button>
            <button
              onClick={handleSave}
              className="flex items-center gap-1 text-xs px-4 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-medium transition"
            >
              {savedSuccess ? (
                <>
                  <Check className="w-3.5 h-3.5" />
                  <span>已儲存</span>
                </>
              ) : (
                <span>儲存設定</span>
              )}
            </button>
          </div>
        </div>

      </div>
    </div>
  );
}
