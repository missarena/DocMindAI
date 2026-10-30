
import { useState, useEffect, useCallback } from "react";
import { askQuestion } from "./services/chatService";
import ReactMarkdown from "react-markdown";

import {
  getAllDocuments,
  uploadDocument,
    getDocumentById,
    deleteDocument
} from "./services/documentService";

import {
  BookOpen,
  Database,
  FileText,
  UploadCloud,
  Search,
  Settings,
  Sparkles,
  Send,
  Sun,
  Moon,
  Layers,
  ExternalLink,
    X,
    Trash2
} from "lucide-react";

function App() {
  // -------------------- STATES --------------------

  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");

  const [activeTab, setActiveTab] = useState("All Documents");
  const [search, setSearch] = useState("");
  const [dark, setDark] = useState(true);
  const [question, setQuestion] = useState("");

  const [selectedDocument, setSelectedDocument] = useState(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [showDetails, setShowDetails] = useState(false);

  const [messages, setMessages] = useState([]);
  const [sending, setSending] = useState(false);
  const [chatError, setChatError] = useState("");
  const [conversationId, setConversationId] = useState(null);
  const [activeDocumentId, setActiveDocumentId] = useState(null);

  // -------------------- FETCH DOCUMENTS --------------------

  const fetchDocuments = useCallback(async () => {
    try {
      setLoading(true);
      setError("");

      const response = await getAllDocuments();

      if (response.success) {
        setDocuments(response.data || []);
      } else {
        setError(
            response.message || "Unable to fetch documents"
        );
      }
    } catch (err) {
      console.error("Fetch documents error:", err);

      setError(
          err.response?.data?.message ||
          "Could not connect to the backend"
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  // -------------------- UPLOAD DOCUMENTS --------------------

  const handleUpload = async (event) => {
    const files = Array.from(event.target.files || []);

    if (files.length === 0) return;

    try {
      setUploading(true);
      setError("");

      for (const file of files) {
        await uploadDocument(file);
      }

      await fetchDocuments();

      alert("Document uploaded and indexed successfully!");
    } catch (err) {
      console.error("Upload error:", err);

      setError(
          err.response?.data?.message ||
          err.message ||
          "Document upload failed"
      );
    } finally {
      setUploading(false);
      event.target.value = "";
    }
  };


  // -------------------- FILTER DOCUMENTS --------------------

  const filteredDocuments = documents.filter((doc) =>
      doc.filename?.toLowerCase().includes(search.toLowerCase())
  );

  const handleViewDetails = async (id) => {
    try {
      setSelectedDocument(null)
      setDetailsLoading(true);
      setShowDetails(true);

      const response = await getDocumentById(id);

      if (response.success) {
        setSelectedDocument(response.data);
      } else {
        setError(response.message || "Unable to fetch document details");
        setShowDetails(false);
      }
    } catch (err) {
      console.error("Document details error:", err);
      setError("Unable to fetch document details");
      setShowDetails(false);
    } finally {
      setDetailsLoading(false);
    }
  };

  const handleDeleteDocument = async () => {
    if (!selectedDocument) return;

    const confirmed = window.confirm(
        `Are you sure you want to delete "${selectedDocument.filename}"?`
    );

    if (!confirmed) return;

    try {
      await deleteDocument(selectedDocument.id);

      setShowDetails(false);
      setSelectedDocument(null);

      await fetchDocuments();
      alert("Document deleted successfully!");
    } catch (err) {
      console.error("Delete document error:", err);
      setError(
          err.response?.data?.message || "Failed to delete document"
      );
    }
  };


  const handleSendMessage = async (text = question) => {
    const trimmedQuestion = text.trim();

    if (!trimmedQuestion || sending) return;

    const userMessage = {
      role: "user",
      content: trimmedQuestion,
    };

    setMessages((prev) => [...prev, userMessage]);
    setQuestion("");
    setSending(true);
    setChatError("");

    try {
      const payload = {
        question: trimmedQuestion,
        documentId: activeDocumentId,
        topK: 5,
        minSimilarity: 0.0,
        ...(conversationId ? { conversationId } : {}),
      };

      const response = await askQuestion(payload);

      if (!response.success) {
        throw new Error(response.message || "Chat request failed");
      }

      const result = response.data;

      setConversationId(result.conversationId);

      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          content: result.answer,
          citations: result.citations || [],
          responseTimeMs: result.responseTimeMs,
        },
      ]);
    } catch (err) {
      console.error("Chat error:", err);

      setChatError(
          err.response?.data?.message ||
          err.message ||
          "Unable to get a response. Please try again."
      );
    } finally {
      setSending(false);
    }
  };


  // -------------------- UI --------------------

  return (
      <div
          className={
            dark
                ? "h-screen overflow-hidden bg-[#070b19] text-slate-100"
                : "h-screen overflow-hidden bg-slate-100 text-slate-900"
          }
      >
        {/* TOP NAVIGATION */}

        <header className="flex h-[72px] shrink-0 items-center justify-between border-b border-slate-800 px-6">

          <div className="flex items-center gap-3">

            <div className="rounded-xl bg-blue-600 p-2.5">
              <BookOpen size={23} />
            </div>

            <div>
              <h1 className="text-xl font-bold text-blue-400">
                DocMind
              </h1>

              <p className="text-xs text-slate-400">
                Spring AI • PostgreSQL pgvector • Ollama
              </p>
            </div>

            <span className="ml-2 rounded-full border border-blue-500/40 bg-blue-500/10 px-3 py-1 text-xs text-blue-400">
            ✦ AI RAG
          </span>

          </div>

          <div className="flex items-center gap-4">

          <span className="hidden items-center gap-2 rounded-full border border-slate-700 px-4 py-2 text-sm text-slate-300 sm:flex">
            <span className="h-2 w-2 rounded-full bg-blue-500" />
            Scope: All Documents ({documents.length})
          </span>

            <button
                className="flex items-center gap-2 rounded-lg border border-slate-700 px-3 py-2 text-sm hover:bg-slate-800"
                title="Semantic search will be connected later"
            >
              <Search size={16} />
              <span className="hidden sm:inline">
              Search Chunks
            </span>
            </button>

            <button
                onClick={() =>
                    window.open(
                        "http://localhost:8081/swagger-ui/index.html",
                        "_blank"
                    )
                }
                className="hidden items-center gap-2 text-sm text-slate-300 hover:text-blue-400 md:flex"
            >
              Swagger API
              <ExternalLink size={14} />
            </button>

            <button
                onClick={() => setDark(!dark)}
                className="rounded-lg border border-slate-700 p-2 hover:bg-slate-800"
            >
              {dark ? <Sun size={18} /> : <Moon size={18} />}
            </button>

          </div>
        </header>

        {/* MAIN LAYOUT */}

        <div className="flex h-[calc(100vh-72px)] overflow-hidden">

          {/* SIDEBAR */}

          <aside className="hidden w-[305px] shrink-0 overflow-y-auto border-r border-slate-800 bg-[#0b1223] p-4 md:block">

            {/* KNOWLEDGE BASE */}

            <div className="mb-5 flex items-center gap-3 px-2">

              <Database className="text-blue-400" size={21} />

              <div>
                <h2 className="text-sm font-semibold">
                  Knowledge Base
                </h2>

                <p className="text-xs text-slate-400">
                  Indexed vector documents
                </p>
              </div>

            </div>

            {/* UPLOAD AREA */}

            <label
                className={`mb-6 flex flex-col items-center rounded-2xl border border-dashed border-slate-600 bg-slate-900/50 px-4 py-7 text-center transition ${
                    uploading
                        ? "cursor-not-allowed opacity-60"
                        : "cursor-pointer hover:border-blue-500 hover:bg-blue-500/5"
                }`}
            >

              <UploadCloud
                  size={30}
                  className="mb-3 text-blue-400"
              />

              <span className="text-sm font-semibold text-blue-400">
              {uploading
                  ? "Uploading and indexing..."
                  : "Upload document"}
            </span>

              <span className="mt-1 text-xs text-slate-400">
              or drag files here
            </span>

              <span className="mt-2 text-[11px] text-slate-500">
              PDF, Word, Markdown, TXT, CSV
            </span>

              <input
                  type="file"
                  className="hidden"
                  multiple
                  accept=".pdf,.doc,.docx,.md,.txt,.csv"
                  onChange={handleUpload}
                  disabled={uploading}
              />

            </label>

            {/* ALL DOCUMENTS */}

            <button
                onClick={() => {
                  setActiveTab("All Documents");
                  setActiveDocumentId(null);
                  setMessages([]);
                  setConversationId(null);
                }}
                className={`mb-4 flex w-full items-center gap-3 rounded-xl border p-3 text-left ${
                    activeTab === "All Documents"
                        ? "border-blue-500/40 bg-blue-500/10"
                        : "border-transparent hover:bg-slate-800"
                }`}
            >

              <FileText
                  className="text-blue-400"
                  size={20}
              />

              <div className="flex-1">
                <p className="text-sm font-semibold">
                  All Documents
                </p>

                <p className="text-xs text-slate-400">
                  Search across complete corpus
                </p>
              </div>

              <span className="rounded-full bg-slate-700 px-2 py-1 text-xs">
              {documents.length}
            </span>

            </button>

            {/* SEARCH DOCUMENTS */}

            <div className="relative mb-4">

              <Search
                  size={15}
                  className="absolute left-3 top-3 text-slate-500"
              />

              <input
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Filter files..."
                  className="w-full rounded-lg border border-slate-700 bg-[#070b19] py-2 pl-9 pr-3 text-sm outline-none focus:border-blue-500"
              />

            </div>

            {/* DOCUMENT LIST HEADER */}

            <div className="mb-3 flex justify-between text-[11px] font-semibold uppercase tracking-wider text-slate-500">

              <span>Documents</span>
              <span>Status</span>

            </div>

            {/* LOADING */}

            {loading && (
                <p className="mb-3 text-sm text-slate-400">
                  Loading documents...
                </p>
            )}

            {/* ERROR */}

            {error && (
                <div className="mb-3 rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-xs text-red-400">
                  {error}
                  <button
                      onClick={fetchDocuments}
                      className="ml-2 underline"
                  >
                    Retry
                  </button>
                </div>
            )}

            {/* EMPTY STATE */}

            {!loading && filteredDocuments.length === 0 && (
                <p className="text-xs text-slate-500">
                  {search
                      ? "No matching documents found."
                      : "No documents found. Upload your first document."}
                </p>
            )}

            {/* DOCUMENT CARDS */}

            <div className="space-y-3">

              {filteredDocuments.map((doc) => (

                  <button
                      key={doc.id}
                      onClick={() => {
                        setActiveTab(doc.filename);
                        setActiveDocumentId(doc.id);
                        setMessages([]);
                        setConversationId(null);
                        handleViewDetails(doc.id);
                      }
                  }
                      className={`w-full rounded-xl border p-3 text-left transition ${
                          activeTab === doc.filename
                              ? "border-blue-500/50 bg-blue-500/10"
                              : "border-slate-800 bg-[#10192c] hover:border-slate-600"
                      }`}
                  >

                    <div className="flex items-center gap-2">

                      <FileText
                          size={18}
                          className="shrink-0 text-slate-400"
                      />

                      <span className="truncate text-xs font-semibold">
                    {doc.filename}
                  </span>

                    </div>

                    <p className="mt-2 text-[11px] text-slate-500">
                      {((doc.fileSize || 0) / 1024).toFixed(1)} KB
                      {" • "}
                      {doc.totalPages ?? 0} pages
                    </p>

                    <div className="mt-3 flex items-center justify-between border-t border-slate-800 pt-2">

                  <span className="flex items-center gap-1 text-xs text-slate-400">

                    <Layers size={13} />

                    {doc.totalChunks ?? 0} chunks

                  </span>

                      <span
                          className={`text-[11px] ${
                              doc.status === "INDEXED"
                                  ? "text-emerald-400"
                                  : doc.status === "FAILED"
                                      ? "text-red-400"
                                      : "text-yellow-400"
                          }`}
                      >
                    ● {doc.status}
                  </span>

                    </div>

                  </button>

              ))}

            </div>

            {/* SETTINGS */}

            <div className="mt-6 border-t border-slate-800 pt-4">

              <button className="flex w-full items-center gap-3 rounded-lg p-2 text-sm text-slate-400 hover:bg-slate-800">

                <Settings size={17} />

                Settings

              </button>

            </div>

          </aside>

          {/* MAIN CHAT AREA */}

          <main className="flex min-w-0 flex-1 flex-col overflow-hidden">

            {/* CONTEXT BAR */}

            <div className="flex h-10 shrink-0 items-center justify-between border-b border-slate-800 px-6 text-xs">

            <span className="flex items-center gap-2 text-slate-400">

              <span className="h-2 w-2 rounded-full bg-emerald-400" />

              Context:

              <strong className="text-slate-200">
                {activeTab === "All Documents"
                    ? "All Uploaded Documents"
                    : activeTab}
              </strong>

            </span>

              <span className="text-slate-500">
              Grounded RAG
            </span>

            </div>

            {/* WELCOME SCREEN */}


            {/* CHAT MESSAGES / WELCOME SCREEN */}

            <section className="flex min-h-0 flex-1 flex-col overflow-y-auto px-5 py-8">

              {messages.length === 0 ? (

                  <div className="flex flex-1 flex-col items-center justify-center text-center">

                    <div className="mb-5 rounded-2xl bg-blue-600 p-4 shadow-lg shadow-blue-900/30">
                      <Sparkles size={30} />
                    </div>

                    <h2 className="text-2xl font-bold">
                      DocMind AI Document Assistant
                    </h2>

                    <p className="mt-3 max-w-xl text-sm leading-6 text-slate-400">
                      Upload PDF reports, Word files, or notes and chat
                      naturally with grounded facts, source citations,
                      and page numbers.
                    </p>

                    <div className="mt-8 grid w-full max-w-[590px] grid-cols-1 gap-3 sm:grid-cols-2">

                      {[
                        {
                          icon: "📄",
                          title: "Summarize Key Points",
                          desc: "Summarize the key points and executive highlights of this document.",
                        },
                        {
                          icon: "❓",
                          title: "Interview / Q&A Insights",
                          desc: "What are the most critical questions and answers covered in this text?",
                        },
                        {
                          icon: "📝",
                          title: "Action Items & Deadlines",
                          desc: "List all actionable takeaways, deadlines, and responsibilities mentioned.",
                        },
                        {
                          icon: "🔍",
                          title: "Technical Concepts",
                          desc: "Explain the main technical concepts and architecture described in this document.",
                        },
                      ].map((item) => (

                          <button
                              key={item.title}
                              onClick={() => handleSendMessage(item.desc)}
                              disabled={sending}
                              className="rounded-xl border border-slate-800 bg-[#10192c] p-4 text-left transition hover:border-blue-500/50 disabled:opacity-50"
                          >
                            <h3 className="text-sm font-semibold text-blue-400">
                              {item.icon} {item.title}
                            </h3>

                            <p className="mt-2 text-xs leading-5 text-slate-400">
                              {item.desc}
                            </p>
                          </button>

                      ))}

                    </div>
                  </div>

              ) : (

                  <div className="mx-auto flex w-full max-w-[850px] flex-col gap-6">

                    {messages.map((message, index) => (

                        <div
                            key={index}
                            className={`flex ${
                                message.role === "user"
                                    ? "justify-end"
                                    : "justify-start"
                            }`}
                        >

                          <div
                              className={`max-w-[90%] rounded-2xl p-4 sm:max-w-[85%] ${
                                  message.role === "user"
                                      ? "bg-blue-600 text-white"
                                      : "border border-slate-700 bg-[#10192c] text-slate-200"
                              }`}
                          >

                            <p className="mb-2 text-xs font-semibold opacity-70">
                              {message.role === "user" ? "You" : "DocMind AI"}
                            </p>

                            {message.role === "assistant" ? (

                                <div className="text-sm leading-7 [&_h1]:mb-3 [&_h1]:text-xl [&_h1]:font-bold [&_h2]:mb-2 [&_h2]:mt-5 [&_h2]:text-lg [&_h2]:font-semibold [&_h3]:mt-4 [&_h3]:font-semibold [&_p]:mb-3 [&_ul]:mb-3 [&_ul]:list-disc [&_ul]:pl-6 [&_ol]:mb-3 [&_ol]:list-decimal [&_ol]:pl-6 [&_li]:mb-1 [&_strong]:font-bold [&_table]:w-full [&_table]:border-collapse [&_th]:border [&_th]:border-slate-600 [&_th]:p-2 [&_td]:border [&_td]:border-slate-600 [&_td]:p-2">

                                  <ReactMarkdown>
                                    {message.content}
                                  </ReactMarkdown>

                                </div>

                            ) : (

                                <p className="whitespace-pre-wrap text-sm leading-6">
                                  {message.content}
                                </p>

                            )}

                            {/* SOURCE CITATIONS */}

                            {message.role === "assistant" &&
                                message.citations?.length > 0 && (

                                    <div className="mt-5 border-t border-slate-700 pt-4">

                                      <p className="mb-3 flex items-center gap-2 text-xs font-semibold text-blue-400">
                                        <FileText size={14} />
                                        Sources ({message.citations.length})
                                      </p>

                                      <div className="space-y-2">

                                        {message.citations.map((citation, citationIndex) => (

                                            <div
                                                key={`${citation.documentId}-${citation.chunkIndex}-${citationIndex}`}
                                                className="rounded-lg border border-slate-700 bg-[#070b19] p-3"
                                            >

                                              <div className="flex items-center gap-2 text-xs font-medium text-slate-200">

                                                <FileText size={14} className="text-blue-400" />

                                                <span className="break-all">
                          {citation.fileName}
                        </span>

                                              </div>

                                              <div className="mt-2 flex flex-wrap gap-3 text-[11px] text-slate-400">

                                                <span>Page {citation.pageNumber}</span>

                                                <span>Chunk {citation.chunkIndex}</span>

                                                {typeof citation.similarityScore === "number" && (
                                                    <span>
                            Similarity: {(
                                                        citation.similarityScore * 100
                                                    ).toFixed(1)}%
                          </span>
                                                )}

                                              </div>

                                              {citation.snippet && (
                                                  <p className="mt-2 text-xs leading-5 text-slate-400">
                                                    {citation.snippet}
                                                  </p>
                                              )}

                                            </div>

                                        ))}

                                      </div>
                                    </div>
                                )}

                          </div>

                        </div>

                    ))}

                    {/* LOADING INDICATOR */}

                    {sending && (

                        <div className="flex justify-start">

                          <div className="rounded-xl border border-slate-700 bg-[#10192c] p-4">

                            <div className="flex items-center gap-3 text-sm text-slate-400">

                              <span className="h-2 w-2 animate-pulse rounded-full bg-blue-400" />

                              DocMind is thinking...

                            </div>

                          </div>

                        </div>

                    )}

                    {/* ERROR MESSAGE */}

                    {chatError && (

                        <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-400">
                          {chatError}
                        </div>

                    )}

                  </div>

              )}

            </section>


            {/* CHAT INPUT */}

            <div className="shrink-0 border-t border-slate-800 bg-[#0c1426] px-5 py-5">

              <div className="mx-auto max-w-[780px]">

                <div className="flex items-center gap-3 rounded-2xl border border-slate-700 bg-[#070b19] p-2 focus-within:border-blue-500">

                  <input
                      value={question}
                      onChange={(e) => setQuestion(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" && !e.shiftKey) {
                          e.preventDefault();
                          handleSendMessage();
                        }
                      }}
                      placeholder={
                        activeDocumentId
                            ? `Ask about ${activeTab}...`
                            : "Ask a question across all documents..."
                      }
                      className="min-w-0 flex-1 bg-transparent px-3 py-2 text-sm outline-none placeholder:text-slate-500"
                  />
                  <button
                      onClick={() => handleSendMessage()}
                      disabled={!question.trim() || sending}
                      title="Send message"
                      className="rounded-xl bg-blue-600 p-3 text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    <Send size={18} />
                  </button>

                </div>

                <div className="mt-2 flex justify-between px-2 text-[11px] text-slate-500">

                <span>
                  Press Enter to send, Shift+Enter for newline
                </span>

                  <span className="flex items-center gap-1 text-blue-400">

                  <Sparkles size={12} />

                  Grounded RAG

                </span>

                </div>

              </div>

            </div>

          </main>

        </div>

        {showDetails && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
              <div className="w-full max-w-lg rounded-2xl border border-slate-700 bg-slate-900 p-6 text-white shadow-2xl">

                <div className="mb-6 flex items-center justify-between">
                  <h2 className="text-xl font-semibold">Document Details</h2>

                  <button onClick={() => setShowDetails(false)}>
                    <X size={22} />
                  </button>
                </div>

                {detailsLoading ? (
                    <p className="text-slate-400">Loading document details...</p>
                ) : selectedDocument ? (
                    <>
                      <div className="space-y-4 text-sm">
                        <p><span className="text-slate-400">Filename:</span> {selectedDocument.filename}</p>
                        <p><span className="text-slate-400">Content Type:</span> {selectedDocument.contentType}</p>
                        <p><span className="text-slate-400">File Size:</span> {selectedDocument.fileSize} bytes</p>
                        <p><span className="text-slate-400">Total Pages:</span> {selectedDocument.totalPages}</p>
                        <p><span className="text-slate-400">Total Chunks:</span> {selectedDocument.totalChunks}</p>
                        <p><span className="text-slate-400">Status:</span> {selectedDocument.status}</p>
                        <p><span className="text-slate-400">Created At:</span> {selectedDocument.createdAt}</p>
                        <p><span className="text-slate-400">Updated At:</span> {selectedDocument.updatedAt}</p>

                        {selectedDocument.errorMessage && (
                            <p className="text-red-400">
                              Error: {selectedDocument.errorMessage}
                            </p>
                        )}
                      </div>

                      <div className="mt-7 flex justify-end">
                        <button
                            onClick={handleDeleteDocument}
                            className="flex items-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm hover:bg-red-700"
                        >
                          <Trash2 size={16} />
                          Delete Document
                        </button>
                      </div>
                    </>
                ) : null}
              </div>
            </div>
        )}

      </div>
  );
}

export default App;
