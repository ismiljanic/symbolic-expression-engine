"use client";

if (typeof document !== "undefined" && !document.getElementById("jb-mono-font")) {
  const link = document.createElement("link");
  link.id = "jb-mono-font";
  link.rel = "stylesheet";
  link.href = "https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600&display=swap";
  document.head.appendChild(link);
}

import { useState, useMemo } from "react";
import { motion, AnimatePresence } from "motion/react";
import { ChevronDown, ChevronUp, Copy, Check, AlertCircle, Loader2, Sparkles, Zap } from "lucide-react";

interface FactorizedGroup {
  power: string;
  terms: string[];
}

interface DeterminantResponse {
  symbolicDeterminant?: string;
  expandedPolynomial?: string;
  factorized?: FactorizedGroup[];
  numericFromSymbolic?: number;
  numericFromLU?: number;
  detectedVariables?: string[];
  luSteps?: string[];
  error?: string;
}

const PREDEFINED_MATRICES: Record<number, string[][]> = {
  4: [
    ["6*(x_l-2)+3*(x_l+1) - lambda", "3*t_l", "0", "-1*(t_l-2)*(t_l-1)*t_l"],
    ["-3", "6*(x_l-2)-2*(x_l) - lambda", "t_l-1", "(t_l-2)*(t_l-1)*(x_l+1)"],
    ["0", "-5", "6*(x_l-2)-5*(x_l-1) - lambda", "-1*(t_l-2)*((x_l)*(x_l+1)+(d/3))"],
    ["0", "0", "-6", "(x_l-1)*x_l*(x_l+1)+((x_l-1)+(x_l+1))*(d/2) - lambda"],
  ],
  5: [
    ["10*(x_l-3) + 6*(x_l+1) - lambda", "6*t_l", "0", "0", "(t_l-3)*(t_l-2)*(t_l-1)*t_l"],
    ["-4", "10*(x_l-3) - 1*(x_l) - lambda", "3*(t_l-1)", "0", "-1*(t_l-3)*(t_l-2)*(t_l-1)*(x_l+1)"],
    ["0", "-7", "10*(x_l-3) - 6*(x_l-1) - lambda", "t_l-2", "(t_l-3)*(t_l-2)*((x_l)*(x_l+1)+(d/3))"],
    ["0", "0", "-9", "10*(x_l-3) - 9*(x_l-2) - lambda", "-1*(t_l-3)*((x_l-1)*x_l*(x_l+1)+((x_l-1)+(x_l+1))*(d/2))"],
    ["0", "0", "0", "-10", "(x_l-2)*(x_l-1)*x_l*(x_l+1)+(((5*(x_l+1))+(3*(x_l-3)))*((x_l-2)+x_l)+(6*(x_l-1)*x_l)-2*(x_l-3)*(x_l-2)+2*d)*(d/10) - lambda"],
  ],
};

function extractNumericVariables(expr: string): string[] {
  const found = new Set<string>();
  const normalized = expr.replace(/λ/g, "lambda");
  if (/\bx\b/.test(normalized)) found.add("x");
  if (/\bl\b/.test(normalized)) found.add("l");
  if (/\bd\b/.test(normalized)) found.add("d");
  if (/\blambda\b/.test(normalized)) found.add("lambda");
  return Array.from(found);
}

const API_BASE = import.meta.env.VITE_API_BASE || "";

const PROGRESS_STAGES = [
  { threshold: 20, label: "Parsing matrix structure...", duration: 300 },
  { threshold: 45, label: "Expanding symbolic expression...", duration: 400 },
  { threshold: 70, label: "Simplifying polynomial...", duration: 500 },
  { threshold: 90, label: "Factorizing expression...", duration: 400 },
  { threshold: 100, label: "LU validation complete!", duration: 300 },
];

type TokenKind =
  | "var_x" | "var_l" | "var_d" | "var_lambda"
  | "var_t" | "number" | "operator" | "paren" | "whitespace" | "other";

interface Token { kind: TokenKind; value: string }

function tokenize(expr: string): Token[] {
  const tokens: Token[] = [];
  let i = 0;

  const isWord = (ch: string | undefined) => ch != null && /[a-zA-Z0-9_]/.test(ch);

  while (i < expr.length) {
    const ch = expr[i];
    const cc = expr.charCodeAt(i);

    // λ (U+03BB) or middle dot · (U+00B7) handled by code point
    if (cc === 0x03BB) { // λ
      tokens.push({ kind: "var_lambda", value: ch }); i++; continue;
    }
    if (cc === 0x00B7) { // · middle dot
      tokens.push({ kind: "operator", value: ch }); i++; continue;
    }

    // lambda keyword
    if (expr.slice(i, i + 6) === "lambda" && !isWord(expr[i + 6])) {
      tokens.push({ kind: "var_lambda", value: "lambda" }); i += 6; continue;
    }
    // x_l compound token
    if (expr.slice(i, i + 3) === "x_l" && !isWord(expr[i + 3])) {
      tokens.push({ kind: "var_x", value: "x_l" }); i += 3; continue;
    }
    // standalone x
    if (ch === "x" && !isWord(expr[i + 1]) && !isWord(expr[i - 1])) {
      tokens.push({ kind: "var_x", value: "x" }); i++; continue;
    }
    // t_l compound token
    if (expr.slice(i, i + 3) === "t_l" && !isWord(expr[i + 3])) {
      tokens.push({ kind: "var_t", value: "t_l" }); i += 3; continue;
    }
    // standalone l
    if (ch === "l" && !isWord(expr[i + 1]) && !isWord(expr[i - 1])) {
      tokens.push({ kind: "var_l", value: "l" }); i++; continue;
    }
    // standalone d
    if (ch === "d" && !isWord(expr[i + 1]) && !isWord(expr[i - 1])) {
      tokens.push({ kind: "var_d", value: "d" }); i++; continue;
    }
    // numbers
    if (/[0-9]/.test(ch)) {
      let num = "";
      while (i < expr.length && /[0-9.]/.test(expr[i])) { num += expr[i]; i++; }
      tokens.push({ kind: "number", value: num }); continue;
    }
    // ASCII operators
    if ("+-*/^".includes(ch)) {
      tokens.push({ kind: "operator", value: ch }); i++; continue;
    }
    // parens
    if (ch === "(" || ch === ")") {
      tokens.push({ kind: "paren", value: ch }); i++; continue;
    }
    // whitespace
    if (/\s/.test(ch)) {
      let ws = "";
      while (i < expr.length && /\s/.test(expr[i])) { ws += expr[i]; i++; }
      tokens.push({ kind: "whitespace", value: ws }); continue;
    }
    // anything else — consume until a known boundary char
    let other = "";
    while (i < expr.length) {
      const c = expr[i];
      const code = expr.charCodeAt(i);
      if (/[\s+\-*/^()0-9]/.test(c) || code === 0x03BB || code === 0x00B7) break;
      other += c; i++;
    }
    if (other) tokens.push({ kind: "other", value: other });
    else i++; // safety advance
  }
  return tokens;
}

const TOKEN_STYLES: Record<TokenKind, string> = {
  var_x: "text-sky-600 font-medium",
  var_l: "text-violet-600 font-medium",
  var_d: "text-amber-600 font-medium",
  var_lambda: "text-emerald-600 font-medium",
  var_t: "text-rose-500 font-medium",
  number: "text-slate-700",
  operator: "text-slate-400",
  paren: "text-slate-500",
  whitespace: "",
  other: "text-slate-700",
};

function MathExpr({ expr, className = "" }: { expr: string; className?: string }) {
  const tokens = useMemo(() => tokenize(expr), [expr]);
  return (
    <span className={className} style={{ fontFamily: "'JetBrains Mono', 'Fira Code', monospace" }}>
      {tokens.map((tok, i) => (
        tok.kind === "whitespace"
          ? <span key={i}>{tok.value}</span>
          : <span key={i} className={TOKEN_STYLES[tok.kind]}>{tok.value}</span>
      ))}
    </span>
  );
}

export function CalculatorInterface() {
  const [matrixSize, setMatrixSize] = useState(2);
  const [matrixValues, setMatrixValues] = useState<string[][]>(
    Array.from({ length: 2 }, () => Array(2).fill(""))
  );
  const [numericInputs, setNumericInputs] = useState({ x: "", l: "", d: "", lambda: "" });
  const [factorizeOption, setFactorizeOption] = useState(false);
  const [detailedLUOption, setDetailedLUOption] = useState(false);
  const [lastResponse, setLastResponse] = useState<DeterminantResponse | null>(null);
  const [showNumeric, setShowNumeric] = useState(false);
  const [copiedSection, setCopiedSection] = useState<string | null>(null);
  const [numericError, setNumericError] = useState<string | null>(null);
  const [isEvaluating, setIsEvaluating] = useState(false);
  const [computeProgress, setComputeProgress] = useState(0);
  const [progressLabel, setProgressLabel] = useState("");

  // Cell edit modal
  const [editingCell, setEditingCell] = useState<{ i: number; j: number } | null>(null);
  const [editingValue, setEditingValue] = useState("");

  // Results toggles
  const [expandedOpen, setExpandedOpen] = useState(true);
  const [factorizedOpen, setFactorizedOpen] = useState(true);
  const [expandedTerms, setExpandedTerms] = useState<Set<number>>(new Set());
  const [showFactorizedTerms, setShowFactorizedTerms] = useState<Set<number>>(new Set());

  const detectedVars = useMemo(() => {
    if (!lastResponse?.expandedPolynomial) return [];
    return extractNumericVariables(lastResponse.expandedPolynomial);
  }, [lastResponse?.expandedPolynomial]);

  const isPureNumeric = detectedVars.length === 0 && lastResponse?.numericFromLU != null;

  const updateMatrixCell = (i: number, j: number, value: string) => {
    const newMatrix = matrixValues.map((row) => [...row]);
    newMatrix[i][j] = value;
    setMatrixValues(newMatrix);
    setShowNumeric(false);
  };

  const handleSizeChange = (size: number) => {
    setMatrixSize(size);
    setMatrixValues(Array.from({ length: size }, () => Array(size).fill("")));
    setShowNumeric(false);
  };

  const loadPredefined = () => {
    if (PREDEFINED_MATRICES[matrixSize]) {
      setMatrixValues(PREDEFINED_MATRICES[matrixSize].map((row) => [...row]));
      setShowNumeric(false);
    }
  };

  // Only open modal when cell already has a value
  const openCellModal = (i: number, j: number) => {
    if (!matrixValues[i][j]) return;
    setEditingCell({ i, j });
    setEditingValue(matrixValues[i][j]);
  };

  const confirmCellEdit = () => {
    if (!editingCell) return;
    updateMatrixCell(editingCell.i, editingCell.j, editingValue);
    setEditingCell(null);
  };

  const cancelCellEdit = () => setEditingCell(null);

  const copyToClipboard = (text: string, section: string) => {
    navigator.clipboard.writeText(text);
    setCopiedSection(section);
    setTimeout(() => setCopiedSection(null), 2000);
  };

  const computeSymbolic = async () => {
    setIsEvaluating(true);
    setComputeProgress(0);
    setProgressLabel("Starting...");

    const animateProgress = async () => {
      for (const stage of PROGRESS_STAGES) {
        await new Promise((r) => setTimeout(r, stage.duration));
        setComputeProgress(stage.threshold);
        setProgressLabel(stage.label);
      }
    };

    const animationPromise = animateProgress();
    const payload: any = { matrix: matrixValues, factorize: factorizeOption, detailedLU: detailedLUOption };

    try {
      const res = await fetch(`${API_BASE}/api/determinant`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      await animationPromise;
      setComputeProgress(100);
      const data: DeterminantResponse = await res.json();
      if (res.ok) {
        setLastResponse(data);
        setShowNumeric(data.numericFromLU != null || (data.detectedVariables?.length ?? 0) > 0);
        setExpandedTerms(new Set());
        setShowFactorizedTerms(new Set());
        setNumericError(null);
      } else {
        setLastResponse({ error: data.error || `HTTP ${res.status}` });
        setShowNumeric(false);
      }
    } catch (err: any) {
      await animationPromise;
      setComputeProgress(100);
      setLastResponse({ error: "Network error: " + err.message });
      setShowNumeric(false);
    }
    setIsEvaluating(false);
    setTimeout(() => setComputeProgress(0), 800);
  };

  const computeNumeric = async () => {
    setNumericError(null);
    setIsEvaluating(true);
    const payload: any = { matrix: matrixValues, factorize: factorizeOption, detailedLU: detailedLUOption };
    if (detectedVars.includes("x")) payload.xValue = Number(numericInputs.x);
    if (detectedVars.includes("l")) payload.lValue = Number(numericInputs.l);
    if (detectedVars.includes("d")) payload.dValue = Number(numericInputs.d);
    if (detectedVars.includes("lambda")) payload.lambdaValue = Number(numericInputs.lambda);
    try {
      const res = await fetch(`${API_BASE}/api/determinant`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        setNumericError(`Server error: ${res.status} - ${await res.text()}`);
        setIsEvaluating(false);
        return;
      }
      const data: DeterminantResponse = await res.json();
      if (data.error) setNumericError(data.error);
      else { setLastResponse((prev) => ({ ...prev, ...data })); setNumericError(null); }
    } catch (err: any) {
      setNumericError("Network error: " + err.message);
    } finally {
      setIsEvaluating(false);
    }
  };

  const toggleTerm = (idx: number) => {
    setExpandedTerms((prev) => {
      const next = new Set(prev);
      next.has(idx) ? next.delete(idx) : next.add(idx);
      return next;
    });
  };

  const escHtml = (s?: string) =>
    String(s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

  return (
    <>
      {/* ── Page ─────────────────────────────────────────────────────────── */}
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-50 p-6">
        <div className="max-w-6xl mx-auto">

          {/* Header */}
          <div className="mb-8">
            <h1 className="text-4xl font-bold text-slate-900 mb-2">Determinant Analysis</h1>
            <p className="text-slate-600">Symbolic computation &amp; polynomial factorization</p>
          </div>

          {/* ── Matrix Input ───────────────────────────────────────────────── */}
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 mb-6">
            <h2 className="text-lg font-semibold text-slate-900 mb-4">
              Matrix Input (
              <motion.span
                key={matrixSize}
                initial={{ opacity: 0, y: -6 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.2 }}
                className="inline-block"
              >
                {matrixSize}×{matrixSize}
              </motion.span>
              )
            </h2>

            {/* Size selector */}
            <div className="flex gap-2 mb-6 flex-wrap">
              {[2, 3, 4, 5, 6].map((n) => (
                <motion.button
                  key={n}
                  onClick={() => handleSizeChange(n)}
                  className={`relative px-4 py-2 rounded font-medium cursor-pointer text-sm overflow-hidden ${matrixSize === n ? "text-white" : "text-slate-700"
                    }`}
                  style={{ background: matrixSize === n ? undefined : "transparent" }}
                  whileHover={{ scale: 1.04 }}
                  whileTap={{ scale: 0.95 }}
                  transition={{ type: "spring", stiffness: 500, damping: 30 }}
                >
                  {matrixSize === n ? (
                    <motion.span
                      layoutId="size-pill"
                      className="absolute inset-0 rounded bg-blue-600 shadow-sm"
                      transition={{ type: "spring", stiffness: 400, damping: 30 }}
                    />
                  ) : (
                    <motion.span
                      className="absolute inset-0 rounded bg-slate-100"
                      transition={{ duration: 0.15 }}
                    />
                  )}
                  <span className="relative z-10">{n}×{n}</span>
                </motion.button>
              ))}
              <motion.button
                onClick={loadPredefined}
                className="relative px-4 py-2 rounded font-medium cursor-pointer text-sm text-amber-900 overflow-hidden"
                whileHover={{ scale: 1.04 }}
                whileTap={{ scale: 0.95 }}
                transition={{ type: "spring", stiffness: 500, damping: 30 }}
              >
                <motion.span
                  className="absolute inset-0 rounded bg-amber-100"
                  transition={{ duration: 0.15 }}
                />
                <span className="relative z-10">Load Predefined</span>
              </motion.button>
            </div>

            {/* Matrix grid */}
            <motion.div
              className="grid gap-2 mb-4"
              style={{ gridTemplateColumns: `repeat(${matrixSize}, minmax(0, 1fr))` }}
              layout
              transition={{ type: "spring", stiffness: 300, damping: 30 }}
            >
              <AnimatePresence mode="popLayout">
                {matrixValues.flatMap((row, i) =>
                  row.map((val, j) => (
                    <motion.div
                      key={`${matrixSize}-${i}-${j}`}
                      initial={{ opacity: 0, scale: 0.85 }}
                      animate={{ opacity: 1, scale: 1 }}
                      exit={{ opacity: 0, scale: 0.85 }}
                      transition={{
                        type: "spring",
                        stiffness: 380,
                        damping: 28,
                        delay: (i * matrixSize + j) * 0.018,
                      }}
                      layout
                      className="relative group"
                    >
                      {val ? (
                        /* Filled cell — click opens modal */
                        <motion.div
                          onClick={() => openCellModal(i, j)}
                          className="relative cursor-pointer"
                          whileTap={{ scale: 0.97 }}
                        >
                          <motion.div
                            className="w-full border border-slate-200 rounded-lg px-2 py-2 text-sm text-center bg-slate-50 text-slate-900 min-h-[36px] flex items-center justify-center overflow-hidden"
                            whileHover={{
                              borderColor: "#d4eaf5",
                              backgroundColor: "#ffffff",
                              boxShadow: "0 1px 6px rgba(59,130,246,0.10)",
                            }}
                            transition={{ type: "spring", stiffness: 400, damping: 28 }}
                          >
                            <span className="truncate w-full text-center block leading-tight">
                              <MathExpr expr={val} className="text-xs" />
                            </span>
                          </motion.div>
                          {/* Expand hint badge */}
                          <motion.div
                            className="absolute -top-1.5 -right-1.5 w-4 h-4 bg-blue-500 rounded-full flex items-center justify-center pointer-events-none"
                            initial={{ opacity: 0, scale: 0 }}
                            whileHover={{ opacity: 1, scale: 1 }}
                            transition={{ duration: 0.15 }}
                          >
                            <svg width="8" height="8" viewBox="0 0 8 8" fill="none">
                              <path d="M1 7L7 1M4.5 1H7V3.5" stroke="white" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" />
                            </svg>
                          </motion.div>
                        </motion.div>
                      ) : (
                        /* Empty cell — inline input, no modal */
                        <motion.input
                          value={val}
                          onChange={(e) => updateMatrixCell(i, j, e.target.value)}
                          placeholder={`${i + 1},${j + 1}`}
                          className="w-full border border-slate-200 rounded-lg px-2 py-2 text-xs text-center font-mono bg-slate-50 text-slate-900 placeholder-slate-300 focus:outline-none min-h-[36px]"
                          whileHover={{ borderColor: "#94a3b8", backgroundColor: "#f8fafc" }}
                          whileFocus={{
                            borderColor: "#3b82f6",
                            backgroundColor: "#ffffff",
                            boxShadow: "0 0 0 3px rgba(59,130,246,0.15)",
                            scale: 1.02,
                          }}
                          transition={{ type: "spring", stiffness: 400, damping: 28 }}
                        />
                      )}
                    </motion.div>
                  ))
                )}
              </AnimatePresence>
            </motion.div>

            {/* Options */}
            <div className="flex gap-6 mb-6">
              <label className="flex items-center gap-3 cursor-pointer select-none">
                <div className="relative">
                  <input
                    type="checkbox"
                    checked={factorizeOption}
                    onChange={(e) => setFactorizeOption(e.target.checked)}
                    className="peer absolute w-5 h-5 opacity-0 cursor-pointer"
                  />
                  <div className="w-5 h-5 rounded border border-slate-300 bg-white flex items-center justify-center peer-checked:bg-blue-600 peer-checked:border-blue-600 transition-colors duration-200 peer-focus:ring-2 peer-focus:ring-blue-500">
                    {factorizeOption && (
                      <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                      </svg>
                    )}
                  </div>
                </div>
                <span className="text-sm font-medium text-slate-700 hover:text-slate-900 transition-colors">
                  Factorize Result
                </span>
              </label>
            </div>

            {/* Compute button */}
            <div className="relative flex flex-col items-start gap-3 w-full">
              <motion.button
                onClick={computeSymbolic}
                disabled={isEvaluating}
                className="relative w-full overflow-hidden px-6 py-3 rounded-lg font-semibold text-white cursor-pointer disabled:cursor-not-allowed"
                style={{
                  background: isEvaluating
                    ? "linear-gradient(135deg, #3b82f6, #2563eb)"
                    : lastResponse
                      ? "linear-gradient(135deg, #10b981, #059669)"
                      : "linear-gradient(135deg, #3b82f6, #2563eb)",
                  boxShadow: "0 4px 6px rgba(0,0,0,0.1)",
                }}
                whileHover={!isEvaluating ? { boxShadow: "0 8px 15px rgba(0,0,0,0.2)", y: -2 } : {}}
                whileTap={!isEvaluating ? { y: 1, boxShadow: "0 4px 6px rgba(0,0,0,0.1)" } : {}}
                transition={{ type: "spring", stiffness: 400, damping: 25 }}
              >
                {isEvaluating && (
                  <motion.div
                    className="absolute inset-0 bg-blue-400 opacity-30"
                    initial={{ scaleX: 0 }}
                    animate={{ scaleX: computeProgress / 100 }}
                    style={{ transformOrigin: "left" }}
                    transition={{ duration: 0.3, ease: "easeOut" }}
                  />
                )}
                <div className="relative flex items-center justify-center gap-2">
                  {!isEvaluating && !lastResponse && (<><Zap className="w-4 h-4" /> Compute Symbolic</>)}
                  {isEvaluating && (<><Loader2 className="w-4 h-4 animate-spin" /> {progressLabel} ({computeProgress}%)</>)}
                  {lastResponse && !isEvaluating && (
                    <>
                      <motion.div initial={{ scale: 0 }} animate={{ scale: 1 }} transition={{ type: "spring", stiffness: 500, damping: 20 }}>
                        <Sparkles className="w-4 h-4" />
                      </motion.div>
                      Recompute
                    </>
                  )}
                </div>
              </motion.button>

              <AnimatePresence>
                {isEvaluating && (
                  <motion.div
                    initial={{ opacity: 0, y: 4 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -4 }}
                    className="flex flex-col gap-1 text-xs text-slate-500 w-full"
                  >
                    {PROGRESS_STAGES.map(({ threshold, label }) => (
                      <motion.div
                        key={label}
                        animate={{ opacity: computeProgress >= threshold ? 1 : 0.3 }}
                        className={`flex items-center gap-1.5 ${computeProgress >= threshold ? "text-blue-600" : ""}`}
                      >
                        <div className={`w-1.5 h-1.5 rounded-full ${computeProgress >= threshold ? "bg-blue-500" : "bg-slate-300"}`} />
                        {label}
                      </motion.div>
                    ))}
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          </div>

          {/* ── Results ──────────────────────────────────────────────────────── */}
          {!lastResponse && (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 text-center">
              <p className="text-blue-900 text-lg">∇ Enter matrix and press Compute</p>
            </div>
          )}

          {lastResponse?.error && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-6">
              <p className="text-red-900 font-semibold">{lastResponse.error}</p>
            </div>
          )}

          {lastResponse && !lastResponse.error && (
            <div className="space-y-4">

              {/* Expanded Polynomial */}
              {lastResponse.expandedPolynomial && (
                <div className="bg-white rounded-lg shadow-sm border border-slate-200 overflow-hidden">
                  <div
                    onClick={() => setExpandedOpen(!expandedOpen)}
                    role="button"
                    tabIndex={0}
                    className="w-full px-6 py-4 bg-slate-50 hover:bg-slate-100 border-b border-slate-200 flex items-center justify-between transition-all cursor-pointer"
                  >
                    <div className="flex flex-col gap-1">
                      <h3 className="font-semibold text-slate-900">Expanded Polynomial</h3>
                      <div className="flex items-center gap-3 text-xs" style={{ fontFamily: "'JetBrains Mono', monospace" }}>
                        <span className="text-sky-600">x</span>
                        <span className="text-violet-600">l</span>
                        <span className="text-amber-600">d</span>
                        <span className="text-emerald-600">lambda / λ</span>
                        <span className="text-rose-500">t</span>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={(e) => { e.stopPropagation(); copyToClipboard(lastResponse.expandedPolynomial!, "expanded"); }}
                        className="flex items-center gap-2 px-3 py-1 bg-slate-200 hover:bg-slate-300 rounded text-sm font-medium text-slate-900 transition-all"
                      >
                        {copiedSection === "expanded" ? <><Check size={16} /> Copied</> : <><Copy size={16} /> Copy</>}
                      </button>
                      {expandedOpen ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
                    </div>
                  </div>
                  <AnimatePresence>
                    {expandedOpen && (
                      <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: "auto", opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        transition={{ duration: 0.2 }}
                        className="overflow-hidden"
                      >
                        <div className="p-6 overflow-x-auto border-t border-slate-200">
                          <pre className="text-sm whitespace-pre-wrap break-words leading-relaxed">
                            <MathExpr expr={lastResponse.expandedPolynomial!} />
                          </pre>
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>
              )}

              {/* Factored Form */}
              {lastResponse.factorized && lastResponse.factorized.length > 0 && (
                <div className="bg-white rounded-lg shadow-sm border border-slate-200 overflow-hidden">
                  <button
                    onClick={() => setFactorizedOpen(!factorizedOpen)}
                    className="w-full px-6 py-4 bg-slate-50 hover:bg-slate-100 border-b border-slate-200 flex items-center justify-between transition-all cursor-pointer"
                  >
                    <h3 className="font-semibold text-slate-900">Factored Form — {lastResponse.factorized.length} groups</h3>
                    {factorizedOpen ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
                  </button>
                  <AnimatePresence>
                    {factorizedOpen && (
                      <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: "auto", opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        transition={{ duration: 0.2 }}
                        className="overflow-hidden"
                      >
                        <div className="p-6 space-y-4 border-t border-slate-200">
                          {lastResponse.factorized.map((group, idx) => (
                            <div key={idx} className="border border-slate-200 rounded-lg overflow-hidden">
                              <div
                                onClick={() => toggleTerm(idx)}
                                role="button"
                                tabIndex={0}
                                className="w-full px-4 py-3 bg-slate-50 hover:bg-slate-100 flex items-center justify-between transition-all cursor-pointer"
                              >
                                <span className="font-medium text-slate-900">
                                  Terms with{" "}
                                  <code className="bg-slate-100 px-2 py-1 rounded">
                                    <MathExpr expr={group.power === "" ? "constant" : group.power} className="text-sm" />
                                  </code>
                                  <span className="text-slate-500 ml-2 text-sm">
                                    ({group.terms.length} {group.terms.length === 1 ? "term" : "terms"})
                                  </span>
                                </span>
                                <div className="flex items-center gap-2">
                                  <button
                                    onClick={(e) => { e.stopPropagation(); copyToClipboard(group.terms.join(" + "), `factorized-${idx}`); }}
                                    className="flex items-center gap-1 px-2 py-1 bg-slate-200 hover:bg-slate-300 rounded text-xs font-medium text-slate-900 transition-all"
                                  >
                                    {copiedSection === `factorized-${idx}` ? <Check size={14} /> : <Copy size={14} />}
                                  </button>
                                  {expandedTerms.has(idx) ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
                                </div>
                              </div>
                              <AnimatePresence>
                                {expandedTerms.has(idx) && (
                                  <motion.div
                                    initial={{ height: 0, opacity: 0 }}
                                    animate={{ height: "auto", opacity: 1 }}
                                    exit={{ height: 0, opacity: 0 }}
                                    transition={{ duration: 0.2 }}
                                    className="overflow-hidden bg-slate-50 border-t border-slate-200"
                                  >
                                    <div className="p-4">
                                      <p className="text-xs font-semibold text-slate-400 mb-3 uppercase tracking-wider">Factorized Form</p>
                                      <div className="space-y-2">
                                        {group.terms.map((term, i) => (
                                          <div key={i} className="p-3 bg-white rounded border border-slate-200 break-words">
                                            <MathExpr expr={term} className="text-sm" />
                                          </div>
                                        ))}
                                      </div>
                                    </div>
                                  </motion.div>
                                )}
                              </AnimatePresence>
                            </div>
                          ))}
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>
              )}

              {/* Numeric Evaluation */}
              {(showNumeric || isPureNumeric) && (
                <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
                  <h3 className="text-lg font-semibold text-slate-900 mb-4">Numeric Evaluation</h3>
                  {numericError && (
                    <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex gap-3">
                      <AlertCircle size={20} className="text-red-600 flex-shrink-0 mt-0.5" />
                      <div>
                        <p className="font-semibold text-red-900">Evaluation Error</p>
                        <p className="text-sm text-red-800 mt-1">{numericError}</p>
                      </div>
                    </div>
                  )}
                  {detectedVars.length > 0 && (
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                      {detectedVars.map((v) => (
                        <div key={v}>
                          <label className="block text-sm font-medium text-slate-700 mb-2">{v}</label>
                          <input
                            type="number"
                            value={numericInputs[v as keyof typeof numericInputs] ?? ""}
                            onChange={(e) => setNumericInputs((prev) => ({ ...prev, [v]: e.target.value }))}
                            placeholder="0"
                            className="w-full border border-slate-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                          />
                        </div>
                      ))}
                    </div>
                  )}
                  {detectedVars.length > 0 && (
                    <button
                      onClick={computeNumeric}
                      disabled={isEvaluating}
                      className="w-full bg-green-600 hover:bg-green-700 disabled:bg-green-400 text-white font-semibold py-3 rounded-lg transition-all shadow-sm mb-6 cursor-pointer"
                    >
                      {isEvaluating ? "⟳ Evaluating..." : "⟳ Evaluate Numerically"}
                    </button>
                  )}
                  {(lastResponse.numericFromSymbolic != null || lastResponse.numericFromLU != null) && (
                    <div className="grid md:grid-cols-2 gap-4">
                      {lastResponse.numericFromSymbolic != null && (
                        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                          <h4 className="font-semibold text-blue-900 mb-2">Symbolic Substitution</h4>
                          <p className="font-mono text-lg text-blue-700">{lastResponse.numericFromSymbolic.toFixed(8)}</p>
                        </div>
                      )}
                      {lastResponse.numericFromLU != null && (
                        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                          <h4 className="font-semibold text-green-900 mb-2">Numeric LU</h4>
                          <p className="font-mono text-lg text-green-700">{lastResponse.numericFromLU.toFixed(8)}</p>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}

              {/* Detected Variables */}
              {lastResponse.detectedVariables && lastResponse.detectedVariables.length > 0 && (
                <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
                  <h3 className="text-lg font-semibold text-slate-900 mb-4">
                    Detected Variables ({lastResponse.detectedVariables.length})
                  </h3>
                  <div className="flex flex-wrap gap-2">
                    {lastResponse.detectedVariables.map((v, i) => (
                      <span key={i} className="inline-block bg-blue-100 text-blue-900 px-3 py-1 rounded-full font-mono text-sm">{v}</span>
                    ))}
                  </div>
                </div>
              )}

              {/* LU Steps */}
              {lastResponse.luSteps && lastResponse.luSteps.length > 0 && (
                <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
                  <h3 className="text-lg font-semibold text-slate-900 mb-4">LU Steps ({lastResponse.luSteps.length})</h3>
                  <div className="space-y-3 max-h-96 overflow-y-auto">
                    {lastResponse.luSteps.map((step, i) => (
                      <div key={i} className="bg-slate-50 p-3 rounded border border-slate-200">
                        <p className="font-mono text-sm text-slate-700 break-words">{escHtml(step)}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* ── Cell Edit Modal ───────────────────────────────────────────────────
           Rendered OUTSIDE all page wrappers so fixed positioning is relative
           to the viewport with no stacking-context interference.            */}
      <AnimatePresence>
        {editingCell && (
          <>
            {/* Backdrop */}
            <motion.div
              className="fixed inset-0"
              style={{
                zIndex: 9998,
                backgroundColor: "rgba(15, 23, 42, 0.6)",
                backdropFilter: "blur(8px)",
                WebkitBackdropFilter: "blur(8px)",
              }}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.18 }}
              onClick={cancelCellEdit}
            />

            {/* Modal panel */}
            <div
              className="fixed inset-0 flex items-center justify-center p-6 pointer-events-none"
              style={{ zIndex: 9999 }}
            >
              <motion.div
                className="bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-lg pointer-events-auto overflow-hidden"
                initial={{ opacity: 0, scale: 0.92, y: 16 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.92, y: 16 }}
                transition={{ type: "spring", stiffness: 400, damping: 32 }}
              >
                {/* Header */}
                <div className="px-6 pt-6 pb-4 border-b border-slate-100">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                          Matrix Cell
                        </span>
                        <motion.span
                          className="px-2 py-0.5 bg-blue-50 text-blue-700 text-xs font-bold rounded-md border border-blue-100"
                          initial={{ scale: 0.8, opacity: 0 }}
                          animate={{ scale: 1, opacity: 1 }}
                          transition={{ delay: 0.08, type: "spring", stiffness: 500 }}
                        >
                          row {editingCell.i + 1}, col {editingCell.j + 1}
                        </motion.span>
                      </div>
                      <h3 className="text-lg font-semibold text-slate-900 mt-1">
                        Edit M[{editingCell.i + 1},{editingCell.j + 1}]
                      </h3>
                    </div>
                    <motion.button
                      onClick={cancelCellEdit}
                      className="w-8 h-8 rounded-lg bg-slate-100 flex items-center justify-center text-slate-500 cursor-pointer"
                      whileHover={{ scale: 1.08, backgroundColor: "#e2e8f0" }}
                      whileTap={{ scale: 0.93 }}
                      transition={{ type: "spring", stiffness: 500, damping: 30 }}
                    >
                      <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                        <path d="M1 1L11 11M11 1L1 11" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
                      </svg>
                    </motion.button>
                  </div>

                  {/* Current value preview */}
                  {matrixValues[editingCell.i][editingCell.j] && (
                    <motion.div
                      className="mt-3 px-3 py-2 bg-slate-50 rounded-lg border border-slate-200"
                      initial={{ opacity: 0, y: 4 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.1 }}
                    >
                      <p className="text-xs text-slate-400 mb-0.5">Current value</p>
                      <p className="text-sm break-all">
                        <MathExpr expr={matrixValues[editingCell.i][editingCell.j]} />
                      </p>
                    </motion.div>
                  )}
                </div>

                {/* Body */}
                <div className="px-6 py-5">
                  <label className="block text-sm font-medium text-slate-700 mb-2">New expression</label>
                  <motion.textarea
                    value={editingValue}
                    onChange={(e) => setEditingValue(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) confirmCellEdit();
                      if (e.key === "Escape") cancelCellEdit();
                    }}
                    placeholder={`Enter expression for M[${editingCell.i + 1},${editingCell.j + 1}]…`}
                    rows={3}
                    autoFocus
                    className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm font-mono text-slate-900 bg-slate-50 placeholder-slate-300 focus:outline-none resize-none"
                    initial={{ borderColor: "#e2e8f0" }}
                    whileFocus={{
                      borderColor: "#3b82f6",
                      backgroundColor: "#ffffff",
                      boxShadow: "0 0 0 3px rgba(59,130,246,0.12)",
                    }}
                    transition={{ duration: 0.15 }}
                  />
                  <p className="text-xs text-slate-400 mt-2">
                    Use <code className="bg-slate-100 px-1 rounded">*</code> for multiplication,{" "}
                    <code className="bg-slate-100 px-1 rounded">^</code> for powers.{" "}
                    <kbd className="bg-slate-100 px-1 rounded text-slate-500">Click</kbd> to confirm.
                  </p>
                </div>

                {/* Footer */}
                <div className="px-6 pb-6 flex gap-3 justify-end">
                  <motion.button
                    onClick={cancelCellEdit}
                    className="px-4 py-2 rounded-lg text-sm font-medium text-slate-600 bg-slate-100 cursor-pointer"
                    whileHover={{ backgroundColor: "#e2e8f0", scale: 1.02 }}
                    whileTap={{ scale: 0.97 }}
                    transition={{ type: "spring", stiffness: 500, damping: 30 }}
                  >
                    Cancel
                  </motion.button>
                  <motion.button
                    onClick={confirmCellEdit}
                    className="px-5 py-2 rounded-lg text-sm font-semibold text-white bg-blue-600 cursor-pointer"
                    whileHover={{ scale: 1.03, backgroundColor: "#2563eb" }}
                    whileTap={{ scale: 0.97 }}
                    transition={{ type: "spring", stiffness: 500, damping: 30 }}
                  >
                    Confirm
                  </motion.button>
                </div>
              </motion.div>
            </div>
          </>
        )}
      </AnimatePresence>
    </>
  );
}