import React, { useState, useEffect, useRef } from 'react';
import { marked } from 'marked';
import { Sparkles, User, Bot, CheckCircle2, AlertCircle, AlertTriangle, Edit2, RotateCw, Square, Copy, Check } from 'lucide-react';

export default function ChatArea({
  currentConversation,
  originalDraftText,
  messages,
  isLoading,
  errorInfo,
  apiKeyConfigured,
  onOpenSettings,
  onSampleClick,
  onRenameTitle,
  onStopThinking,
  onRetry
}) {
  const scrollEndRef = useRef(null);
  const [isEditingTitle, setIsEditingTitle] = useState(false);
  const [titleInput, setTitleInput] = useState('');
  const [copiedId, setCopiedId] = useState(null);

  const effectiveDraft = originalDraftText || (messages && messages.find((m) => m.senderType === 'USER' && m.roundNumber === 1)?.content) || '';

  const handleCopyText = (id, content) => {
    let textToCopy = content || '';
    const trimmed = textToCopy.trim();

    // 若為 JSON 格式，直接提取 refinedText
    if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
      try {
        const obj = JSON.parse(trimmed);
        if (obj.refinedText) {
          const parts = [];
          if (obj.refinedText.casual) parts.push(obj.refinedText.casual);
          if (obj.refinedText.formal) parts.push(obj.refinedText.formal);
          if (parts.length > 0) {
            navigator.clipboard.writeText(parts.join('\n\n'));
            setCopiedId(id);
            setTimeout(() => setCopiedId(null), 1800);
            return;
          }
        }
      } catch (e) {}
    }

    // 優先嘗試提取 blockquote 內的純英文文字
    const blockquoteMatches = textToCopy.match(/^>\s*(.+)$/gm);
    if (blockquoteMatches && blockquoteMatches.length > 0) {
      textToCopy = blockquoteMatches.map(line => line.replace(/^>\s*/, '').trim()).join('\n\n');
    }
    navigator.clipboard.writeText(textToCopy);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 1800);
  };

  useEffect(() => {
    // 若只有第 1 輪修飾，不強制觸底滾動，讓使用者的視覺焦點保持在頂部原始草稿與 AI 卡片起點
    const isRoundOne = messages.length <= 2;
    if (isRoundOne) {
      return;
    }
    // 後續追加問答（Round 2 以上）平滑滾動至底部追蹤對話
    scrollEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isLoading, errorInfo]);

  useEffect(() => {
    if (currentConversation) {
      setTitleInput(currentConversation.title);
    }
  }, [currentConversation]);

  const handleSaveTitle = () => {
    if (titleInput.trim() && currentConversation) {
      onRenameTitle(currentConversation.id, titleInput.trim());
    }
    setIsEditingTitle(false);
  };

  const handleKeyDownTitle = (e) => {
    if (e.key === 'Enter') handleSaveTitle();
    if (e.key === 'Escape') setIsEditingTitle(false);
  };

  const renderMarkdown = (content) => {
    if (!content) return { __html: '' };
    let textToRender = content;

    // 若為尚未被後端轉換的純 JSON 結構，前端亦具備容錯轉換能力
    const trimmed = content.trim();
    if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
      try {
        const obj = JSON.parse(trimmed);
        if (obj.refinedText || obj.grammarAnalysis || obj.rationale || obj.reply) {
          if (obj.reply) {
            textToRender = obj.reply;
          } else {
            let md = '### 🌟 修飾後英文全文 (Refined English Text)\n\n';
            if (obj.refinedText?.casual) {
              md += `🗣️ **自然道地口語版 (Natural & Conversational)**\n> ${obj.refinedText.casual.replace(/\n/g, '\n> ')}\n\n`;
            }
            if (obj.refinedText?.formal) {
              md += `💼 **專業商務正式版 (Professional & Formal)**\n> ${obj.refinedText.formal.replace(/\n/g, '\n> ')}\n\n`;
            }
            md += '---\n\n';
            if (Array.isArray(obj.grammarAnalysis) && obj.grammarAnalysis.length > 0) {
              md += '### 🔍 語病與道地性解析 (Chinglish & Grammar Analysis)\n';
              obj.grammarAnalysis.forEach(item => {
                if (item.original || item.explanation) {
                  md += `- **${item.original || ''}** ➔ ${item.explanation || ''}\n`;
                }
              });
              md += '\n';
            }
            if (Array.isArray(obj.rationale) && obj.rationale.length > 0) {
              md += '### 💡 修改原因與語境解析 (Rationale & Insights)\n';
              obj.rationale.forEach(r => {
                const txt = typeof r === 'string' ? r : (r.text || '');
                if (txt) md += `- ${txt}\n`;
              });
              md += '\n';
            }
            if (Array.isArray(obj.idiomsAndUpgrades) && obj.idiomsAndUpgrades.length > 0) {
              md += '### 📚 實用道地片語與延伸替換 (Idioms & Upgrades)\n';
              obj.idiomsAndUpgrades.forEach(item => {
                if (item.phrase) {
                  md += `- **${item.phrase}**：${item.meaning || ''}\n`;
                  if (item.example) md += `  - *例句*：${item.example}\n`;
                }
              });
            }
            textToRender = md;
          }
        }
      } catch (e) {
        // partial JSON or parse error, keep original text
      }
    }

    try {
      const html = marked.parse(textToRender || '');
      return { __html: html };
    } catch {
      return { __html: textToRender };
    }
  };

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-[var(--background)]">
      {/* Top Title Bar (Option C: Dual Renaming) */}
      <div className="h-10 border-b border-[var(--border)] px-4 sm:px-6 flex items-center justify-between bg-[var(--card)]/60 text-xs shrink-0 select-none">
        <div className="flex items-center gap-2 min-w-0 flex-1">
          <span className="text-[var(--muted)]">主題：</span>
          {isEditingTitle ? (
            <input
              type="text"
              value={titleInput}
              onChange={(e) => setTitleInput(e.target.value)}
              onBlur={handleSaveTitle}
              onKeyDown={handleKeyDownTitle}
              autoFocus
              className="px-2 py-0.5 text-xs rounded border border-indigo-500 bg-[var(--background)] text-[var(--foreground)] w-64 focus:outline-none"
            />
          ) : (
            <div
              onClick={() => currentConversation && setIsEditingTitle(true)}
              className="flex items-center gap-1.5 min-w-0 cursor-pointer group py-1 px-1.5 rounded hover:bg-[var(--background)] transition"
              title="點擊就地修改標題"
            >
              <span className="font-semibold truncate text-[var(--foreground)]">
                {currentConversation ? currentConversation.title : '新對話 (未命名)'}
              </span>
              {currentConversation && (
                <Edit2 className="w-3 h-3 text-[var(--muted)] opacity-0 group-hover:opacity-100 transition shrink-0" />
              )}
            </div>
          )}
        </div>

        <div className="text-[11px] text-[var(--muted)] hidden sm:inline">
          大原則：一次新對話修飾一篇英文文本，後續針對該篇深入討論
        </div>
      </div>

      {/* Messages Scroll Area */}
      <div className="flex-1 overflow-y-auto p-4 sm:p-6 md:p-8 space-y-6 max-w-4xl mx-auto w-full">
        {(!messages || messages.length === 0) ? (
          /* Empty State */
          <div className="h-full flex flex-col items-center justify-center text-center p-6 space-y-5 my-auto">
            <div className="w-16 h-16 rounded-3xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center border border-indigo-500/20 shadow-xs">
              <Sparkles className="w-8 h-8" />
            </div>

            <div>
              <h2 className="text-xl font-bold tracking-tight text-[var(--foreground)]">
                輸入需要修飾的英文文本
              </h2>
              <p className="mt-1.5 text-xs text-[var(--muted)] max-w-md mx-auto leading-relaxed">
                單次上限 1,000 字元。首次送出後將由「英文修飾 Skill」深度解析不口語處與修改原因，後續可在同一對話持續深入追加討論。
              </p>
            </div>

            {/* Mock Mode Alert in Empty State */}
            {!apiKeyConfigured && (
              <div className="bg-amber-500/10 border border-amber-500/30 rounded-2xl p-4 text-left text-xs space-y-2 text-amber-800 dark:text-amber-200 max-w-lg w-full flex items-start justify-between gap-3 shadow-2xs">
                <div className="space-y-1">
                  <div className="font-semibold flex items-center gap-1.5 text-amber-700 dark:text-amber-400">
                    <AlertTriangle className="w-4 h-4 text-amber-500 shrink-0" />
                    <span>目前處於 Mock 本地模擬模式</span>
                  </div>
                  <p className="text-[11px] text-amber-700/90 dark:text-amber-300/90 leading-relaxed">
                    尚未配置 Gemini API Key。送出後將回傳預設的專案延期測試假資料，無法理解真實文本。請設定 Key 以啟用真實 Gemini 2.5 Flash 智能修飾！
                  </p>
                </div>
                <button
                  onClick={onOpenSettings}
                  className="px-2.5 py-1.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-white font-semibold text-xs whitespace-nowrap shadow-xs transition cursor-pointer shrink-0"
                >
                  設定 Key ⚙️
                </button>
              </div>
            )}

            {/* Guidelines */}
            <div className="bg-[var(--card)] border border-[var(--border)] rounded-2xl p-4 text-left text-xs space-y-2 text-[var(--muted)] max-w-lg w-full">
              <div className="font-semibold text-[var(--foreground)] flex items-center gap-1.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                <span>操作規則與快捷鍵提醒：</span>
              </div>
              <ul className="list-disc pl-5 space-y-1">
                <li>在輸入框按 <kbd className="px-1.5 py-0.5 rounded bg-[var(--background)] border border-[var(--border)] font-mono">Enter</kbd> 可持續向下換行。</li>
                <li>按下 <kbd className="px-1.5 py-0.5 rounded bg-[var(--background)] border border-[var(--border)] font-mono">Shift + Enter</kbd> 或點選「送出」將文本發送。</li>
                <li>AI 思考時若想調整，可隨時點擊「⏹️ 停止生成」中斷並還原文字。</li>
              </ul>
            </div>

            {/* Quick Templates */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 max-w-lg w-full text-left">
              <button
                onClick={() => onSampleClick("I am writing this email for inform you about the project status. Because our team meet some difficult problem in database connection, so the launch date maybe delay for two weeks. Please kindly understand our situation.")}
                className="p-3 rounded-xl border border-[var(--border)] bg-[var(--card)] hover:border-indigo-500 hover:shadow-xs transition cursor-pointer text-xs space-y-1 text-left"
              >
                <div className="font-semibold text-[var(--foreground)]">💼 專案延期商務通知</div>
                <div className="text-[var(--muted)] line-clamp-2">"Because our team meet some difficult problem in database connection, so..."</div>
              </button>

              <button
                onClick={() => onSampleClick("Dear Hiring Manager, I am very excited to apply for this software engineer position. I have 3 years experience and I can do many things like Java and React. Hope you can give me chance to interview.")}
                className="p-3 rounded-xl border border-[var(--border)] bg-[var(--card)] hover:border-indigo-500 hover:shadow-xs transition cursor-pointer text-xs space-y-1 text-left"
              >
                <div className="font-semibold text-[var(--foreground)]">📄 外商求職自薦信</div>
                <div className="text-[var(--muted)] line-clamp-2">"I am very excited to apply for this software engineer position. I have 3 years experience..."</div>
              </button>
            </div>
          </div>
        ) : (
          <>
            {/* Mock Mode Alert Banner when conversation is active */}
            {!apiKeyConfigured && (
              <div className="bg-amber-500/10 border border-amber-500/30 rounded-2xl p-3.5 text-xs flex items-center justify-between gap-3 text-amber-800 dark:text-amber-200 mb-4 animate-in fade-in shadow-2xs">
                <div className="flex items-center gap-2 min-w-0">
                  <AlertTriangle className="w-4 h-4 text-amber-500 shrink-0" />
                  <span className="leading-tight">
                    <strong>目前處於 Mock 本地模擬模式</strong>：以下內容為固定測試假資料（專案延期），無法針對您的真實草稿分析。
                  </span>
                </div>
                <button
                  onClick={onOpenSettings}
                  className="px-2.5 py-1 rounded-lg bg-amber-500 hover:bg-amber-600 text-white font-medium text-xs whitespace-nowrap shadow-xs transition cursor-pointer shrink-0"
                >
                  填入 Key 啟用真實 AI ↗
                </button>
              </div>
            )}

            {/* Top Pinned / Persistent Original English Draft Card */}
            {effectiveDraft && (
              <div className="bg-[var(--card)] border-2 border-indigo-500/30 dark:border-indigo-500/20 rounded-2xl p-4 sm:p-5 shadow-xs transition mb-4">
                <div className="flex items-center justify-between pb-3 border-b border-[var(--border)] mb-3 text-xs">
                  <div className="flex items-center gap-2">
                    <span className="p-1 rounded-md bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 font-bold">
                      📝
                    </span>
                    <span className="font-bold text-xs sm:text-sm text-[var(--foreground)] tracking-tight">
                      修改前原始英文草稿 (Original English Draft)
                    </span>
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 border border-indigo-500/20">
                      基準文本
                    </span>
                  </div>

                  <button
                    onClick={() => handleCopyText('original-draft-top', effectiveDraft)}
                    className="flex items-center gap-1.5 text-xs text-[var(--muted)] hover:text-indigo-600 dark:hover:text-indigo-400 font-medium cursor-pointer transition py-1 px-2.5 rounded-lg hover:bg-[var(--background)] border border-transparent hover:border-[var(--border)]"
                    title="點擊複製修改前原始草稿"
                  >
                    {copiedId === 'original-draft-top' ? (
                      <>
                        <Check className="w-3.5 h-3.5 text-emerald-500" />
                        <span className="text-emerald-500 font-semibold">已複製原文</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5" />
                        <span>複製原文草稿</span>
                      </>
                    )}
                  </button>
                </div>

                <div className="text-sm leading-relaxed text-[var(--foreground)] whitespace-pre-wrap select-text font-sans font-medium bg-[var(--background)]/70 p-3.5 rounded-xl border border-[var(--border)]/60">
                  {effectiveDraft}
                </div>
              </div>
            )}

            {/* Render Messages */}
            {messages.map((msg, index) => {
              const isUser = msg.senderType === 'USER';
              const isRound1 = msg.roundNumber === 1;

              // 若已有頂部專屬草稿卡片，第 1 輪的使用者訊息即由頂部卡片代表，不重複顯示冗餘氣泡
              if (isUser && isRound1 && effectiveDraft) {
                return null;
              }

              if (isUser) {
                return (
                  <div key={msg.id || index} className="flex flex-col gap-2">
                    <div className="flex items-center justify-between text-xs text-[var(--muted)] font-medium">
                      <div className="flex items-center gap-2">
                        <span className="flex items-center gap-1">
                          <User className="w-3.5 h-3.5" />
                          <span>您</span>
                        </span>
                        <span>•</span>
                        <span className="px-2 py-0.5 rounded text-[11px] font-semibold border bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20">
                          {`追加發問 (Round ${msg.roundNumber})`}
                        </span>
                      </div>

                      {msg.content && (
                        <button
                          onClick={() => handleCopyText('user-' + (msg.id || index), msg.content)}
                          className="flex items-center gap-1 text-[11px] text-[var(--muted)] hover:text-indigo-600 dark:hover:text-indigo-400 font-medium cursor-pointer transition py-0.5 px-2 rounded-lg hover:bg-[var(--card)] border border-transparent hover:border-[var(--border)]"
                          title="複製追加發問內容"
                        >
                          {copiedId === ('user-' + (msg.id || index)) ? (
                            <>
                              <Check className="w-3 h-3 text-emerald-500" />
                              <span className="text-emerald-500 font-semibold">已複製</span>
                            </>
                          ) : (
                            <>
                              <Copy className="w-3 h-3" />
                              <span>複製內容</span>
                            </>
                          )}
                        </button>
                      )}
                    </div>
                    <div className="bg-[var(--card)] border border-[var(--border)] rounded-2xl p-4 sm:p-5 shadow-xs text-sm leading-relaxed whitespace-pre-wrap font-sans select-text">
                      {msg.content}
                    </div>
                  </div>
                );
              } else {
                const isStreaming = String(msg.id).includes('streaming');
                return (
                  <div key={msg.id || index} className="flex flex-col gap-2">
                    <div className="flex items-center justify-between text-xs text-[var(--muted)] font-medium">
                      <div className="flex items-center gap-2">
                        <span className="flex items-center gap-1 text-indigo-600 dark:text-indigo-400 font-semibold">
                          <Bot className="w-3.5 h-3.5" />
                          <span>AI 英文修飾助手</span>
                        </span>
                        <span>•</span>
                        <span className={`px-2 py-0.5 rounded text-[11px] font-semibold border ${
                          !apiKeyConfigured
                            ? 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/30'
                            : isRound1
                              ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20'
                              : 'bg-purple-500/10 text-purple-600 dark:text-purple-400 border-purple-500/20'
                        }`}>
                          {!apiKeyConfigured
                            ? (isRound1 ? '⚠️ 測試模擬假資料 (Mock AI)' : `⚠️ 模擬追加答覆 (Mock AI · Round ${msg.roundNumber})`)
                            : (isRound1 ? '✨ 英文修飾解析 (Gemini 2.5 Flash)' : `💬 上下文追加答覆 (Round ${msg.roundNumber})`)}
                        </span>
                      </div>

                      <div className="flex items-center gap-2">
                        {!isStreaming && msg.content && (
                          <button
                            onClick={() => handleCopyText(msg.id || index, msg.content)}
                            className="flex items-center gap-1 text-[11px] text-[var(--muted)] hover:text-indigo-600 dark:hover:text-indigo-400 font-medium cursor-pointer transition py-0.5 px-2 rounded-lg hover:bg-[var(--background)] border border-transparent hover:border-[var(--border)]"
                            title="點擊複製修飾後英文全文"
                          >
                            {copiedId === (msg.id || index) ? (
                              <>
                                <Check className="w-3 h-3 text-emerald-500" />
                                <span className="text-emerald-500 font-semibold">已複製英文</span>
                              </>
                            ) : (
                              <>
                                <Copy className="w-3 h-3" />
                                <span>複製修飾英文</span>
                              </>
                            )}
                          </button>
                        )}

                        {isStreaming && (
                          <button
                            onClick={onStopThinking}
                            className="flex items-center gap-1 text-[11px] text-amber-500 hover:text-amber-600 font-medium cursor-pointer"
                          >
                            <Square className="w-3 h-3 fill-current" />
                            <span>停止生成</span>
                          </button>
                        )}
                      </div>
                    </div>
                    <div className="bg-[var(--card)] border border-[var(--border)] rounded-2xl p-5 sm:p-6 shadow-xs prose prose-sm dark:prose-invert max-w-none select-text prose-headings:font-semibold prose-a:text-indigo-600 prose-pre:bg-[var(--background)] prose-pre:border prose-pre:border-[var(--border)] [&_blockquote]:border-l-4 [&_blockquote]:border-indigo-500 [&_blockquote]:bg-indigo-500/10 [&_blockquote]:dark:bg-indigo-500/15 [&_blockquote]:py-3 [&_blockquote]:px-4.5 [&_blockquote]:rounded-r-2xl [&_blockquote]:my-3 [&_blockquote]:not-italic [&_blockquote]:text-[var(--foreground)] [&_blockquote]:font-medium [&_blockquote]:select-text [&_blockquote]:cursor-text [&_blockquote]:shadow-2xs [&_blockquote]:tracking-wide [&_hr]:my-5 [&_hr]:border-[var(--border)]">
                      {!apiKeyConfigured && (
                        <div className="mb-4 p-3 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-800 dark:text-amber-200 text-xs flex items-center justify-between not-prose">
                          <span className="flex items-center gap-1.5 min-w-0">
                            <AlertTriangle className="w-4 h-4 text-amber-500 shrink-0" />
                            <span><strong>Mock 模式提醒</strong>：以下為預設測試範本（專案延期），非真實 AI 針對您草稿的解析。</span>
                          </span>
                          <button
                            onClick={onOpenSettings}
                            className="underline font-semibold cursor-pointer ml-2 shrink-0 hover:text-amber-600"
                          >
                            設定 Key 啟用真實 AI ↗
                          </button>
                        </div>
                      )}
                      {isStreaming && !msg.content ? (
                        <div className="flex items-center gap-2 text-xs text-[var(--muted)] py-2">
                          <div className="flex space-x-1">
                            <div className="w-2 h-2 rounded-full bg-indigo-600 animate-bounce"></div>
                            <div className="w-2 h-2 rounded-full bg-indigo-600 animate-bounce [animation-delay:0.2s]"></div>
                            <div className="w-2 h-2 rounded-full bg-indigo-600 animate-bounce [animation-delay:0.4s]"></div>
                          </div>
                          <span>AI 正在思考並重構英文...</span>
                        </div>
                      ) : (
                        <div className="leading-relaxed text-sm space-y-3 select-text">
                          <div dangerouslySetInnerHTML={renderMarkdown(msg.content)} />
                          {isStreaming && (
                            <span className="inline-block w-2 h-4 ml-1 bg-indigo-500 animate-pulse align-middle" />
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                );
              }
            })}
          </>
        )}

        {/* Error Card with Retry Button (Option 3) */}
        {errorInfo && (
          <div className="flex flex-col gap-2 animate-in fade-in">
            <div className="flex items-center gap-2 text-xs text-red-500 font-semibold">
              <AlertCircle className="w-4 h-4" />
              <span>連線或執行異常 (Error Details)</span>
            </div>
            <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-4 sm:p-5 text-xs space-y-3">
              <div className="text-[var(--foreground)] font-medium">
                後端或 AI 模型回傳了以下錯誤訊息：
              </div>
              <div className="font-mono text-xs whitespace-pre-wrap select-all bg-[var(--background)] p-3 rounded-xl border border-red-500/20 text-red-600 dark:text-red-400 break-words max-h-56 overflow-y-auto leading-relaxed shadow-2xs">
                {errorInfo.message || '無法取得 AI 回應 (連線逾時或 API 速率限制)。'}
              </div>
              <div className="text-[var(--muted)] text-[11px]">
                💡 您的輸入草稿已為您妥善保存，確認錯誤原因後可點擊下方按鈕立即重試。
              </div>
              <button
                onClick={onRetry}
                className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-medium text-xs shadow-xs transition cursor-pointer"
              >
                <RotateCw className="w-3.5 h-3.5" />
                <span>點擊重試 🔄</span>
              </button>
            </div>
          </div>
        )}

        <div ref={scrollEndRef} />
      </div>
    </div>
  );
}
