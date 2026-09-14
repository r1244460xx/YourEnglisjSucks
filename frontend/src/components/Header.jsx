import React from 'react';
import { Sparkles, Globe2, ArrowLeftRight, Settings, Moon, Sun, Key } from 'lucide-react';

export default function Header({ 
  activeTab, 
  setActiveTab, 
  onOpenSettings, 
  apiKeyConfigured, 
  modelName,
  isDark,
  toggleDark 
}) {
  return (
    <header className="h-14 border-b border-[var(--border)] px-4 flex items-center justify-between bg-[var(--card)] shrink-0 z-10 select-none">
      {/* Brand */}
      <div className="flex items-center gap-3">
        <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center text-white font-bold text-sm shadow-sm tracking-tight">
          YS
        </div>
        <div className="flex flex-col">
          <span className="font-bold text-sm tracking-tight text-[var(--foreground)] flex items-center gap-1.5">
            YourEnglishSucks
            <span className="text-[10px] bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 px-1.5 py-0.2 rounded font-mono border border-indigo-500/20">
              v1.0
            </span>
          </span>
        </div>
      </div>

      {/* 3 Horizontal Tabs (Point 2 in requirements) */}
      <div className="flex items-center bg-[var(--background)] p-1 rounded-xl border border-[var(--border)] text-xs sm:text-sm font-medium">
        <button
          onClick={() => setActiveTab('polish')}
          className={`px-3 sm:px-4 py-1.5 rounded-lg flex items-center gap-1.5 transition ${
            activeTab === 'polish'
              ? 'bg-indigo-600 text-white shadow-sm font-semibold'
              : 'text-[var(--muted)] hover:text-[var(--foreground)]'
          }`}
        >
          <Sparkles className="w-3.5 h-3.5" />
          <span>英文修飾</span>
        </button>

        <button
          disabled
          className="px-3 sm:px-4 py-1.5 rounded-lg flex items-center gap-1.5 text-[var(--muted)] opacity-50 cursor-not-allowed"
          title="後續版本實作"
        >
          <Globe2 className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">英翻中</span>
          <span className="text-[9px] uppercase px-1 py-0.2 rounded bg-[var(--card)] border border-[var(--border)]">
            Coming
          </span>
        </button>

        <button
          disabled
          className="px-3 sm:px-4 py-1.5 rounded-lg flex items-center gap-1.5 text-[var(--muted)] opacity-50 cursor-not-allowed"
          title="後續版本實作"
        >
          <ArrowLeftRight className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">中翻英</span>
          <span className="text-[9px] uppercase px-1 py-0.2 rounded bg-[var(--card)] border border-[var(--border)]">
            Coming
          </span>
        </button>
      </div>

      {/* Right Controls */}
      <div className="flex items-center gap-2">
        <div 
          onClick={onOpenSettings}
          className={`cursor-pointer flex items-center gap-1.5 px-2.5 py-1 text-xs rounded-lg border transition ${
            apiKeyConfigured 
              ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20 hover:bg-emerald-500/20' 
              : 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/30 hover:bg-amber-500/20 shadow-xs'
          }`}
          title={apiKeyConfigured ? 'Gemini API Key 已就緒 (真實 AI 模式)' : '目前為 Mock 靜態模擬模式，點擊設定 Gemini API Key'}
        >
          <Key className="w-3 h-3" />
          <span className="font-medium">
            {apiKeyConfigured ? (modelName || 'Gemini 2.5 Flash') : 'Mock 模式 (未設 Key)'}
          </span>
          <span className={`w-1.5 h-1.5 rounded-full ${apiKeyConfigured ? 'bg-emerald-500' : 'bg-amber-500 animate-ping'}`}></span>
        </div>

        <button
          onClick={toggleDark}
          className="p-2 rounded-xl text-[var(--muted)] hover:text-[var(--foreground)] hover:bg-[var(--card)] border border-transparent hover:border-[var(--border)] transition"
          title="切換主題"
        >
          {isDark ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
        </button>

        <button
          onClick={onOpenSettings}
          className="p-2 rounded-xl text-[var(--muted)] hover:text-[var(--foreground)] hover:bg-[var(--card)] border border-transparent hover:border-[var(--border)] transition"
          title="設定"
        >
          <Settings className="w-4 h-4" />
        </button>
      </div>
    </header>
  );
}
