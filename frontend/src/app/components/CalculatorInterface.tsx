"use client";

// ── Font & KaTeX injection ─────────────────────────────────────────────────────
if (typeof document !== "undefined") {
  if (!document.getElementById("jb-mono-font")) {
    const link = document.createElement("link");
    link.id = "jb-mono-font";
    link.rel = "stylesheet";
    link.href = "https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600&family=DM+Serif+Display:ital@0;1&family=DM+Sans:wght@400;500;600&display=swap";
    document.head.appendChild(link);
  }
  if (!document.getElementById("katex-css")) {
    const link = document.createElement("link");
    link.id = "katex-css";
    link.rel = "stylesheet";
    link.href = "https://cdnjs.cloudflare.com/ajax/libs/KaTeX/0.16.9/katex.min.css";
    document.head.appendChild(link);
  }
  if (!document.getElementById("katex-js")) {
    const script = document.createElement("script");
    script.id = "katex-js";
    script.src = "https://cdnjs.cloudflare.com/ajax/libs/KaTeX/0.16.9/katex.min.js";
    document.head.appendChild(script);
  }
}

import { useState, useMemo, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "motion/react";
import {
  ChevronDown, ChevronUp, Copy, Check,
  AlertCircle, Loader2, Sparkles, Zap,
  Grid3x3, BookOpen, FlaskConical
} from "lucide-react";

// ── Types ─────────────────────────────────────────────────────────────────────
interface FactorizedGroup { power: string; terms: string[] }
interface DeterminantResponse {
  symbolicDeterminant?: string;
  expandedPolynomial?: string;
  factorized?: FactorizedGroup[];
  numericFromSymbolic?: number;
  numericFromLU?: number;
  detectedVariables?: string[];
  luSteps?: string[];
  error?: string;
  symbolicMatrix?: string[][];
}

// ── KaTeX renderer ────────────────────────────────────────────────────────────
function KaTeX({ tex, display = false, className = "" }: { tex: string; display?: boolean; className?: string }) {
  const ref = useRef<HTMLSpanElement>(null);
  useEffect(() => {
    const tryRender = () => {
      if (ref.current && (window as any).katex) {
        try {
          (window as any).katex.render(tex, ref.current, {
            displayMode: display,
            throwOnError: false,
            errorColor: "#ef4444",
          });
        } catch { /* ignore */ }
      }
    };
    if ((window as any).katex) {
      tryRender();
    } else {
      const script = document.getElementById("katex-js");
      script?.addEventListener("load", tryRender);
      const timer = setInterval(() => {
        if ((window as any).katex) { clearInterval(timer); tryRender(); }
      }, 100);
      return () => clearInterval(timer);
    }
  }, [tex, display]);
  return <span ref={ref} className={className} />;
}

// ── Predefined matrices ───────────────────────────────────────────────────────
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

const FACTORIZE_VARS = [
  { value: "d", label: "d", tex: "D" },
  { value: "x", label: "x", tex: "x" },
  { value: "l", label: "l", tex: "l" },
  { value: "lam", label: "λ", tex: "\\lambda" },
];

function extractNumericVariables(expr: string): string[] {
  const found = new Set<string>();
  const n = expr.replace(/λ/g, "lambda");
  if (/\bx\b/.test(n)) found.add("x");
  if (/\bl\b/.test(n)) found.add("l");
  if (/\bd\b/.test(n)) found.add("d");
  if (/\blambda\b/.test(n)) found.add("lambda");
  return Array.from(found);
}

// Add here in .env http://localhost:8080 or in ""
const API_BASE = import.meta.env.VITE_API_BASE || "";

const PROGRESS_STAGES = [
  { threshold: 20, label: "Parsing matrix...", duration: 300 },
  { threshold: 45, label: "Expanding expression...", duration: 400 },
  { threshold: 70, label: "Simplifying polynomial...", duration: 500 },
  { threshold: 90, label: "Factorizing...", duration: 400 },
  { threshold: 100, label: "Complete!", duration: 300 },
];

// ── Token colouring ───────────────────────────────────────────────────────────
type TK = "var_x" | "var_l" | "var_d" | "var_lambda" | "var_t" | "number" | "operator" | "paren" | "whitespace" | "other";
interface Tok { kind: TK; value: string }

function tokenize(expr: string): Tok[] {
  const toks: Tok[] = [];
  let i = 0;
  const isW = (ch?: string) => ch != null && /[a-zA-Z0-9_]/.test(ch);
  while (i < expr.length) {
    const ch = expr[i], cc = expr.charCodeAt(i);
    if (cc === 0x03BB) { toks.push({ kind: "var_lambda", value: ch }); i++; continue; }
    if (cc === 0x00B7) { toks.push({ kind: "operator", value: ch }); i++; continue; }
    if (expr.slice(i, i + 6) === "lambda" && !isW(expr[i + 6])) { toks.push({ kind: "var_lambda", value: "lambda" }); i += 6; continue; }
    if (expr.slice(i, i + 3) === "x_l" && !isW(expr[i + 3])) { toks.push({ kind: "var_x", value: "x_l" }); i += 3; continue; }
    if (ch === "x" && !isW(expr[i + 1]) && !isW(expr[i - 1])) { toks.push({ kind: "var_x", value: "x" }); i++; continue; }
    if (expr.slice(i, i + 3) === "t_l" && !isW(expr[i + 3])) { toks.push({ kind: "var_t", value: "t_l" }); i += 3; continue; }
    if (ch === "l" && !isW(expr[i + 1]) && !isW(expr[i - 1])) { toks.push({ kind: "var_l", value: "l" }); i++; continue; }
    if (ch === "d" && !isW(expr[i + 1]) && !isW(expr[i - 1])) { toks.push({ kind: "var_d", value: "d" }); i++; continue; }
    if (/[0-9]/.test(ch)) { let n = ""; while (i < expr.length && /[0-9.]/.test(expr[i])) { n += expr[i]; i++; } toks.push({ kind: "number", value: n }); continue; }
    if ("+-*/^".includes(ch)) { toks.push({ kind: "operator", value: ch }); i++; continue; }
    if (ch === "(" || ch === ")") { toks.push({ kind: "paren", value: ch }); i++; continue; }
    if (/\s/.test(ch)) { let ws = ""; while (i < expr.length && /\s/.test(expr[i])) { ws += expr[i]; i++; } toks.push({ kind: "whitespace", value: ws }); continue; }
    let o = "";
    while (i < expr.length) { const c = expr[i], code = expr.charCodeAt(i); if (/[\s+\-*/^()0-9]/.test(c) || code === 0x03BB || code === 0x00B7) break; o += c; i++; }
    if (o) toks.push({ kind: "other", value: o }); else i++;
  }
  return toks;
}

const TK_CLS: Record<TK, string> = {
  var_x: "text-blue-600 font-semibold", var_l: "text-purple-600 font-semibold",
  var_d: "text-orange-500 font-semibold", var_lambda: "text-emerald-600 font-semibold",
  var_t: "text-rose-500 font-semibold", number: "text-slate-700",
  operator: "text-slate-400", paren: "text-slate-400", whitespace: "", other: "text-slate-700",
};

const MONO: React.CSSProperties = { fontFamily: "'JetBrains Mono','Fira Code',monospace" };

function MathExpr({ expr, className = "" }: { expr: string; className?: string }) {
  const toks = useMemo(() => tokenize(expr), [expr]);
  return (
    <span className={className} style={MONO}>
      {toks.map((t, i) => t.kind === "whitespace"
        ? <span key={i}>{t.value}</span>
        : <span key={i} className={TK_CLS[t.kind]}>{t.value}</span>)}
    </span>
  );
}

// ── Formula Reference with KaTeX ──────────────────────────────────────────────
function FormulaReference({ n }: { n: number }) {
  const coeff1 = n * (n + 1) / 2;

  const rows = Array.from({ length: n }, (_, idx) => {
    const k = idx + 1;
    const c2 = (n * (n - 1) / 2) + ((n + 1 - k) * (n + 1 - k) - n * n);
    const bCoeff = k < n ? -((n - k + 1) * (n - k) / 2) : null;
    const cVal = k > 1 ? (n * (n + 1) / 2 - (n + 1 - k) * (n + 2 - k) / 2) : null;
    const off1 = -n + 1; const off2 = 2 - k;
    const b1 = 1 - k; const b2 = 2 - k;
    return { k, c2, bCoeff, cVal, off1, off2, b1, b2 };
  });

  const fmtOff = (o: number) => o >= 0 ? `+${o}` : `${o}`;

  return (
    <div className="rounded-2xl border-2 border-indigo-100 bg-gradient-to-br from-indigo-50 via-white to-purple-50 overflow-hidden shadow-sm">
      {/* Header */}
      <div className="px-6 py-4 bg-blue-500 flex items-center gap-3">
        <div className="w-8 h-8 rounded-xl flex items-center justify-center">
          <BookOpen size={16} className="text-white" />
        </div>
        <div>
          <h3 className="text-sm font-bold text-white tracking-wide">Matrix Definition</h3>
          <p className="text-xs text-indigo-200">Formulas for each element · n = {n}</p>
        </div>
      </div>

      <div className="p-6 space-y-5">
        {/* x_l substitution */}
        <div className="p-4 rounded-xl bg-white border-2 border-blue-100 shadow-sm">
          <p className="text-xs font-bold text-blue-400 uppercase tracking-widest mb-3">Variable Substitution</p>
          <div className="flex items-center justify-center py-1">
            <KaTeX tex="x_l = x - 2(l-1)l" display={false} className="text-base" />
          </div>
        </div>

        {/* General formulas */}
        <div className="grid gap-3">
          {/* Ak */}
          <div className="rounded-xl border-2 border-blue-100 bg-blue-50/70 overflow-hidden">
            <div className="px-4 py-2 bg-blue-100 flex items-center gap-2">
              <span className="text-xs font-bold text-blue-700 uppercase tracking-wider">Diagonal — A</span>
              <span className="ml-auto text-xs text-blue-500">k ∈ {"{1,…,n}"}</span>
            </div>
            <div className="px-4 py-3">
              <div className="flex justify-center mb-3">
                <KaTeX
                  tex={`A_k = \\frac{n(n+1)}{2}\\,x_{l-n+1} + \\left(\\frac{n(n-1)}{2} + (n+1-k)^2 - n^2\\right)x_{l+2-k} - \\lambda`}
                  display={true}
                />
              </div>
              <div className="flex flex-wrap gap-1.5 justify-center">
                {rows.map(({ k, c2, off1, off2 }) => (
                  <span key={k} className="inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-lg bg-blue-100 text-blue-800 border border-blue-200" style={MONO}>
                    k={k}:&nbsp;
                    <span className="font-bold text-blue-600">{coeff1}</span>·x_l{fmtOff(off1)}&nbsp;+&nbsp;
                    <span className="font-bold text-blue-600">{c2}</span>·x_l{fmtOff(off2)}&nbsp;−&nbsp;λ
                  </span>
                ))}
              </div>
            </div>
          </div>

          {/* Bk */}
          <div className="rounded-xl border-2 border-purple-100 bg-purple-50/70 overflow-hidden">
            <div className="px-4 py-2 bg-purple-100 flex items-center gap-2">
              <span className="text-xs font-bold text-purple-700 uppercase tracking-wider">Upper diagonal — B</span>
              <span className="ml-auto text-xs text-purple-500">k ∈ {"{1,…,n−1}"}</span>
            </div>
            <div className="px-4 py-3">
              <div className="flex justify-center mb-3">
                <KaTeX
                  tex={`B_k = -\\frac{(n-k+1)(n-k)}{2}\\left(x_{l+1-k}\\,x_{l+2-k} - D\\right)`}
                  display={true}
                />
              </div>
              <div className="flex flex-wrap gap-1.5 justify-center">
                {rows.filter(r => r.bCoeff != null).map(({ k, bCoeff, b1, b2 }) => (
                  <span key={k} className="inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-lg bg-purple-100 text-purple-800 border border-purple-200" style={MONO}>
                    k={k}:&nbsp;
                    <span className="font-bold text-purple-600">{bCoeff}</span>·(x_l{fmtOff(b1)}·x_l{fmtOff(b2)}&nbsp;−&nbsp;D)
                  </span>
                ))}
              </div>
            </div>
          </div>

          {/* Ck */}
          <div className="rounded-xl border-2 border-emerald-100 bg-emerald-50/70 overflow-hidden">
            <div className="px-4 py-2 bg-emerald-100 flex items-center gap-2">
              <span className="text-xs font-bold text-emerald-700 uppercase tracking-wider">Lower diagonal — C</span>
              <span className="ml-auto text-xs text-emerald-500">k ∈ {"{2,…,n}"}</span>
            </div>
            <div className="px-4 py-3">
              <div className="flex justify-center mb-3">
                <KaTeX
                  tex={`C_k = \\frac{n(n+1)}{2} - \\frac{(n+1-k)(n+2-k)}{2}`}
                  display={true}
                />
              </div>
              <div className="flex flex-wrap gap-1.5 justify-center">
                {rows.filter(r => r.cVal != null).map(({ k, cVal }) => (
                  <span key={k} className="inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-lg bg-emerald-100 text-emerald-800 border border-emerald-200" style={MONO}>
                    k={k}: <span className="font-bold text-emerald-600">{cVal}</span>
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Recurrence */}
        <div className="rounded-xl border-2 border-orange-100 bg-orange-50/70 overflow-hidden">
          <div className="px-4 py-2 bg-orange-100 flex items-center gap-2">
            <span className="text-xs font-bold text-orange-700 uppercase tracking-wider">Tridiagonal recurrence</span>
          </div>
          <div className="px-4 py-3 flex flex-wrap gap-4 justify-center">
            <KaTeX tex="D_0 = 1" />
            <KaTeX tex="D_1 = A_1" />
            <KaTeX tex="D_k = A_k \cdot D_{k-1} - C_k \cdot B_{k-1} \cdot D_{k-2}" />
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Symbolic Matrix Preview ───────────────────────────────────────────────────
function SymbolicMatrixPreview({ matrix, onCellClick }: {
  matrix: string[][];
  onCellClick: (i: number, j: number, expr: string) => void;
}) {
  const n = matrix.length;
  const cellType = (i: number, j: number) => {
    if (i === j) return "diag";
    if (j === i + 1) return "upper";
    if (j === i - 1) return "lower";
    return "zero";
  };

  const cfg = {
    diag: { bg: "bg-blue-50", border: "border-blue-200", badge: "bg-blue-100 text-blue-700", label: "A", hover: "hover:border-blue-400 hover:shadow-blue-100" },
    upper: { bg: "bg-purple-50", border: "border-purple-200", badge: "bg-purple-100 text-purple-700", label: "B", hover: "hover:border-purple-400 hover:shadow-purple-100" },
    lower: { bg: "bg-emerald-50", border: "border-emerald-200", badge: "bg-emerald-100 text-emerald-700", label: "C", hover: "hover:border-emerald-400 hover:shadow-emerald-100" },
    zero: { bg: "bg-slate-50", border: "border-slate-100", badge: "", label: "", hover: "" },
  } as const;

  return (
    <div className="rounded-2xl border-2 border-slate-100 bg-white overflow-hidden shadow-sm">
      {/* Header */}
      <div className="px-6 py-4 bg-white  flex items-center gap-3">
        <Grid3x3 size={16} className="text-blue-500" />
        <span className="text-sm font-bold text-blue-500">Computed Symbolic Matrix</span>
        <div className="ml-auto flex items-center gap-4">
          {(["diag", "upper", "lower"] as const).map(t => (
            <span key={t} className={`text-xs px-2 py-0.5 rounded-md font-bold ${cfg[t].badge}`}>
              {cfg[t].label}
            </span>
          ))}
          <span className="text-xs text-slate-400 font-mono">{n}×{n}</span>
        </div>
      </div>

      <div className="p-4 overflow-x-auto">
        {/* Column labels */}
        <div className="flex gap-2 mb-2 pl-10">
          {matrix[0].map((_, j) => (
            <div key={j} className="flex-1 min-w-[120px] text-center text-xs font-bold text-slate-400 tracking-wider" style={MONO}>c{j}</div>
          ))}
        </div>

        <div className="space-y-2">
          {matrix.map((row, i) => (
            <motion.div key={i} initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }}
              transition={{ delay: i * 0.05 }} className="flex items-stretch gap-2">
              {/* Row label */}
              <div className="w-9 shrink-0 flex items-center justify-center">
                <span className="text-xs font-bold text-slate-400 bg-slate-100 w-7 h-7 rounded-lg flex items-center justify-center" style={MONO}>r{i}</span>
              </div>
              {/* Cells */}
              {row.map((cell, j) => {
                const type = cellType(i, j);
                const c = cfg[type];
                const isZero = cell === "0";
                const isLong = cell.length > 32;
                return (
                  <motion.div key={j}
                    onClick={() => !isZero && onCellClick(i, j, cell)}
                    className={[
                      "flex-1 min-w-[120px] rounded-xl border-2 p-2 transition-all",
                      c.bg, c.border,
                      !isZero ? `cursor-pointer hover:shadow-md ${c.hover}` : "",
                    ].join(" ")}
                    whileHover={!isZero ? { y: -1, scale: 1.01 } : {}}
                    transition={{ type: "spring", stiffness: 400, damping: 28 }}>
                    {!isZero && c.label && (
                      <div className={`inline-flex text-[10px] font-bold px-1.5 py-0.5 rounded-md mb-1 ${c.badge}`}>
                        {c.label}<sub className="text-[9px]">{i + 1}</sub>
                      </div>
                    )}
                    <div className="text-xs leading-snug">
                      {isZero ? (
                        <span className="text-slate-300 font-bold text-sm">0</span>
                      ) : isLong ? (
                        <span className="flex items-end gap-1">
                          <span className="truncate block" style={{ maxWidth: "calc(100% - 1.25rem)" }}>
                            <MathExpr expr={cell.slice(0, 32) + "…"} />
                          </span>
                        </span>
                      ) : (
                        <MathExpr expr={cell} />
                      )}
                    </div>
                  </motion.div>
                );
              })}
            </motion.div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── Main Component ─────────────────────────────────────────────────────────────
export function CalculatorInterface() {
  const [matrixSize, setMatrixSize] = useState(2);
  const [matrixValues, setMatrixValues] = useState<string[][]>(Array.from({ length: 2 }, () => Array(2).fill("")));
  const [numericInputs, setNumericInputs] = useState({ x: "", l: "", d: "", lambda: "" });

  const [factorizeOption, setFactorizeOption] = useState(false);
  const [factorizeVar, setFactorizeVar] = useState("d");
  const [detailedLUOption, setDetailedLUOption] = useState(false);
  const [useTridiagonal, setUseTridiagonal] = useState(false);
  const [tridiagonalSize, setTridiagonalSize] = useState(3);

  const [lastResponse, setLastResponse] = useState<DeterminantResponse | null>(null);
  const [showNumeric, setShowNumeric] = useState(false);
  const [copiedSection, setCopiedSection] = useState<string | null>(null);
  const [numericError, setNumericError] = useState<string | null>(null);
  const [isEvaluating, setIsEvaluating] = useState(false);
  const [computeProgress, setComputeProgress] = useState(0);
  const [progressLabel, setProgressLabel] = useState("");
  const [showMatrixPreview, setShowMatrixPreview] = useState(false);
  const [showFormulaRef, setShowFormulaRef] = useState(false);
  const [viewingCell, setViewingCell] = useState<{ i: number; j: number; expr: string } | null>(null);
  const [editingCell, setEditingCell] = useState<{ i: number; j: number } | null>(null);
  const [editingValue, setEditingValue] = useState("");

  const [expandedOpen, setExpandedOpen] = useState(true);
  const [factorizedOpen, setFactorizedOpen] = useState(true);
  const [expandedTerms, setExpandedTerms] = useState<Set<number>>(new Set());

  const detectedVars = useMemo(() => {
    if (!lastResponse?.expandedPolynomial) return [];
    return extractNumericVariables(lastResponse.expandedPolynomial);
  }, [lastResponse?.expandedPolynomial]);

  const isPureNumeric = detectedVars.length === 0 && lastResponse?.numericFromLU != null;
  const activeN = useTridiagonal ? tridiagonalSize : matrixSize;

  const updateMatrixCell = (i: number, j: number, value: string) => {
    const m = matrixValues.map(r => [...r]); m[i][j] = value; setMatrixValues(m); setShowNumeric(false);
  };
  const handleSizeChange = (size: number) => {
    setMatrixSize(size); setMatrixValues(Array.from({ length: size }, () => Array(size).fill(""))); setShowNumeric(false);
  };
  const loadPredefined = () => {
    if (PREDEFINED_MATRICES[matrixSize]) {
      setMatrixValues(PREDEFINED_MATRICES[matrixSize].map(r => [...r]));
      setShowNumeric(false); setUseTridiagonal(false);
    }
  };
  const openCellModal = (i: number, j: number) => {
    if (!matrixValues[i][j]) return;
    setEditingCell({ i, j }); setEditingValue(matrixValues[i][j]);
  };
  const confirmCellEdit = () => {
    if (!editingCell) return;
    updateMatrixCell(editingCell.i, editingCell.j, editingValue); setEditingCell(null);
  };
  const copyToClipboard = (text: string, section: string) => {
    navigator.clipboard.writeText(text); setCopiedSection(section);
    setTimeout(() => setCopiedSection(null), 2000);
  };
  const toggleTerm = (idx: number) => {
    setExpandedTerms(prev => { const n = new Set(prev); n.has(idx) ? n.delete(idx) : n.add(idx); return n; });
  };

  const buildPayload = (withNumeric: boolean) => {
    const payload: any = { factorize: factorizeOption, factorizeVar, detailedLU: detailedLUOption };
    if (useTridiagonal) { payload.useTridiagonal = true; payload.tridiagonalSize = tridiagonalSize; }
    else { payload.matrix = matrixValues; }
    if (withNumeric) {
      if (detectedVars.includes("x")) payload.xValue = Number(numericInputs.x);
      if (detectedVars.includes("l")) payload.lValue = Number(numericInputs.l);
      if (detectedVars.includes("d")) payload.dValue = Number(numericInputs.d);
      if (detectedVars.includes("lambda")) payload.lambdaValue = Number(numericInputs.lambda);
    }
    return payload;
  };

  const computeSymbolic = async () => {
    setIsEvaluating(true); setComputeProgress(0); setProgressLabel("Starting...");
    const animateProgress = async () => {
      for (const s of PROGRESS_STAGES) { await new Promise(r => setTimeout(r, s.duration)); setComputeProgress(s.threshold); setProgressLabel(s.label); }
    };
    const ap = animateProgress();
    try {
      const res = await fetch(`${API_BASE}/api/determinant`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(buildPayload(false)) });
      await ap; setComputeProgress(100);
      const data: DeterminantResponse = await res.json();
      if (res.ok) { setLastResponse(data); setShowNumeric(data.numericFromLU != null || (data.detectedVariables?.length ?? 0) > 0); setExpandedTerms(new Set()); setNumericError(null); if (data.symbolicMatrix) setShowMatrixPreview(true); }
      else { setLastResponse({ error: data.error || `HTTP ${res.status}` }); setShowNumeric(false); }
    } catch (err: any) { await ap; setComputeProgress(100); setLastResponse({ error: "Network error: " + err.message }); setShowNumeric(false); }
    setIsEvaluating(false); setTimeout(() => setComputeProgress(0), 800);
  };

  const computeNumeric = async () => {
    setNumericError(null); setIsEvaluating(true);
    try {
      const res = await fetch(`${API_BASE}/api/determinant`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(buildPayload(true)) });
      if (!res.ok) { setNumericError(`Server error: ${res.status}`); setIsEvaluating(false); return; }
      const data: DeterminantResponse = await res.json();
      if (data.error) setNumericError(data.error);
      else { setLastResponse(prev => ({ ...prev, ...data })); setNumericError(null); }
    } catch (err: any) { setNumericError("Network error: " + err.message); }
    finally { setIsEvaluating(false); }
  };

  // ── Cell type styling for the input grid ─────────────────────────────────
  const getCellAccent = (i: number, j: number) => {
    if (i === j) return "border-blue-300 bg-blue-50/60 ring-blue-200";
    if (j === i + 1) return "border-purple-300 bg-purple-50/60 ring-purple-200";
    if (j === i - 1) return "border-emerald-300 bg-emerald-50/60 ring-emerald-200";
    return "border-slate-200 bg-white ring-slate-100";
  };

  return (
    <>
      {/* ── Page ──────────────────────────────────────────────────────────── */}
      <div className="min-h-screen p-6" style={{
        background: "linear-gradient(135deg, #f0f4ff 0%, #ffffff 40%, #fdf4ff 100%)",
        fontFamily: "'DM Sans', sans-serif",
      }}>
        <div className="max-w-6xl mx-auto">

          {/* ── Header ──────────────────────────────────────────────────── */}
          <motion.div className="mb-8" initial={{ opacity: 0, y: -16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}>
            <div className="flex items-start justify-between">
              <div>
                <h1 className="text-5xl font-bold text-slate-900 mb-1" style={{ fontFamily: "'DM Serif Display',serif" }}>
                  Determinant
                  <span className="ml-3 bg-blue-500 bg-clip-text text-transparent">Analysis</span>
                </h1>
                <p className="text-slate-500 text-lg">Symbolic computation · Polynomial factorization · Tridiagonal matrices</p>
              </div>
              <div className="hidden md:flex items-center gap-2 mt-2">
                <span className="w-3 h-3 rounded-full bg-blue-400" />
                <span className="w-3 h-3 rounded-full bg-purple-400" />
                <span className="w-3 h-3 rounded-full bg-emerald-400" />
                <span className="w-3 h-3 rounded-full bg-orange-400" />
              </div>
            </div>
          </motion.div>

          {/* ── Matrix Input Card ──────────────────────────────────────── */}
          <motion.div className="bg-white rounded-3xl shadow-lg border border-slate-100 overflow-hidden mb-6"
            initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4, delay: 0.1 }}>

            {/* Card top bar */}
            <div className="px-7 py-5 flex items-center gap-4 border-b border-slate-100">
              <div className="w-10 h-10 rounded-2xl bg-blue-500 flex items-center justify-center shadow-md">
                <Grid3x3 size={18} className="text-white" />
              </div>
              <div>
                <h2 className="text-base font-bold text-slate-800">Matrix Input</h2>
                <p className="text-xs text-slate-400">Manual entry or symbolic tridiagonal generation</p>
              </div>
              {/* Formulas toggle */}
              <motion.button onClick={() => setShowFormulaRef(v => !v)}
                className={`ml-auto flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-semibold transition-all cursor-pointer border-2 ${showFormulaRef ? "bg-blue-500 border-blue-500 text-white shadow-md" : "bg-white border-blue-200 text-blue-600 hover:border-blue-400"}`}
                whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.97 }}>
                <BookOpen size={14} />
                Formulas
                {showFormulaRef && <Check size={13} />}
              </motion.button>
            </div>

            <div className="p-7">
              {/* Formula reference panel */}
              <AnimatePresence>
                {showFormulaRef && (
                  <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} exit={{ opacity: 0, height: 0 }} className="overflow-hidden mb-7">
                    <FormulaReference n={activeN} />
                  </motion.div>
                )}
              </AnimatePresence>

              {/* ── Size & mode controls ─────────────────────────────── */}
              <div className="flex flex-wrap gap-2 mb-5 items-center">
                {/* Size pills */}
                <div className="flex gap-1.5 p-1.5 bg-slate-100 rounded-2xl">
                  {[2, 3, 4, 5, 6].map(n => (
                    <motion.button key={n} onClick={() => handleSizeChange(n)}
                      className={`relative h-8 px-3.5 rounded-xl text-sm font-bold cursor-pointer overflow-hidden transition-colors ${matrixSize === n && !useTridiagonal ? "text-white" : "text-blue-500 hover:text-slate-700"}`}
                      whileTap={{ scale: 0.94 }}>
                      {matrixSize === n && !useTridiagonal && (
                        <motion.span layoutId="size-pill" className="absolute inset-0 rounded-xl bg-blue-500 shadow-sm" />
                      )}
                      <span className="relative z-10" style={MONO}>{n}×{n}</span>
                    </motion.button>
                  ))}
                </div>

                <motion.button onClick={loadPredefined}
                  className="h-9 px-4 rounded-xl text-sm font-semibold text-blue-600 bg-blue-50 border-2 border-blue-200 hover:border-blue-400 transition-all cursor-pointer"
                  whileHover={{ scale: 1.03 }} whileTap={{ scale: 0.96 }}>
                  Load Predefined
                </motion.button>

                {/* Divider */}
                <div className="h-7 w-px bg-slate-200 mx-1" />

                {/* Tridiagonal toggle */}
                <label className="flex items-center gap-2.5 cursor-pointer select-none">
                  <div className="relative">
                    <input type="checkbox" checked={useTridiagonal}
                      onChange={e => { setUseTridiagonal(e.target.checked); setShowNumeric(false); setLastResponse(null); }}
                      className="peer sr-only" />
                    <div className={`w-11 h-6 rounded-full border-2 transition-all duration-200 ${useTridiagonal ? "bg-purple-500 border-purple-500" : "bg-white border-slate-300"}`}>
                      <div className={`w-4 h-4 rounded-full bg-white shadow-sm absolute top-1 transition-all duration-200 ${useTridiagonal ? "left-5.5" : "left-0.5"}`}
                        style={{ left: useTridiagonal ? "calc(100% - 1.375rem)" : "0.125rem" }} />
                    </div>
                  </div>
                  <span className={`text-sm font-bold transition-colors ${useTridiagonal ? "text-purple-600" : "text-slate-500"}`}>Tridiagonal</span>
                </label>

                {/* n= size picker */}
                <AnimatePresence>
                  {useTridiagonal && (
                    <motion.div initial={{ opacity: 0, width: 0 }} animate={{ opacity: 1, width: "auto" }} exit={{ opacity: 0, width: 0 }}
                      className="flex items-center gap-1 overflow-hidden">
                      <span className="text-xs font-bold text-purple-400 mx-1">n =</span>
                      <div className="flex gap-1 p-1 bg-purple-50 rounded-xl border border-purple-200">
                        {[2, 3, 4, 5, 6, 7, 8, 9, 10].map(n => (
                          <motion.button key={n} onClick={() => setTridiagonalSize(n)}
                            className={`relative w-7 h-7 rounded-lg text-xs font-bold cursor-pointer overflow-hidden transition-colors ${tridiagonalSize === n ? "text-white" : "text-purple-500 hover:text-purple-700"}`}
                            whileTap={{ scale: 0.9 }}>
                            {tridiagonalSize === n && (
                              <motion.span layoutId="tri-pill" className="absolute inset-0 rounded-lg bg-purple-500"
                                transition={{ type: "spring", stiffness: 400, damping: 28 }} />
                            )}
                            <span className="relative z-10" style={MONO}>{n}</span>
                          </motion.button>
                        ))}
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>

              {/* Tridiagonal info banner */}
              <AnimatePresence>
                {useTridiagonal && (
                  <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} exit={{ opacity: 0, height: 0 }} className="overflow-hidden mb-5">
                    <div className="flex items-center gap-3 px-4 py-3 bg-purple-50 border-2 border-purple-200 rounded-2xl text-sm">
                      <FlaskConical size={16} className="text-purple-500 shrink-0" />
                      <span className="text-purple-700">Using <code className="bg-purple-100 px-1.5 py-0.5 rounded-lg font-mono text-xs font-bold">buildSpecialMatrix({tridiagonalSize})</code> — matrix generated server-side</span>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>

              {/* Manual matrix grid */}
              <AnimatePresence>
                {!useTridiagonal && (
                  <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="mb-5">
                    {/* Column labels */}
                    <div className="flex gap-2 mb-2" style={{ paddingLeft: "2.75rem" }}>
                      {Array.from({ length: matrixSize }, (_, j) => (
                        <div key={j} className="flex-1 text-center text-xs font-bold text-slate-300 tracking-widest uppercase" style={MONO}>c{j}</div>
                      ))}
                    </div>
                    <div className="space-y-2">
                      {matrixValues.map((row, i) => (
                        <motion.div key={`row-${matrixSize}-${i}`}
                          initial={{ opacity: 0, x: -12 }} animate={{ opacity: 1, x: 0 }}
                          transition={{ delay: i * 0.04 }} className="flex items-stretch gap-2">
                          {/* Row label */}
                          <div className="w-10 shrink-0 flex items-center justify-center">
                            <span className="text-xs font-bold text-slate-400 bg-slate-100 w-8 h-8 rounded-xl flex items-center justify-center" style={MONO}>r{i}</span>
                          </div>
                          {row.map((val, j) => {
                            const accent = getCellAccent(i, j);
                            return (
                              <motion.div key={`${matrixSize}-${i}-${j}`}
                                initial={{ opacity: 0, scale: 0.85 }} animate={{ opacity: 1, scale: 1 }}
                                transition={{ type: "spring", stiffness: 380, damping: 28, delay: (i * matrixSize + j) * 0.015 }}
                                className="flex-1 min-w-0">
                                {val ? (
                                  <motion.div onClick={() => openCellModal(i, j)}
                                    className={`w-full border-2 rounded-xl px-2 py-2 min-h-[40px] flex items-center justify-center overflow-hidden cursor-pointer ring-1 ${accent}`}
                                    whileHover={{ scale: 1.02, boxShadow: "0 4px 12px rgba(0,0,0,0.08)" }}
                                    transition={{ type: "spring", stiffness: 400, damping: 28 }}>
                                    <span className="truncate text-center block w-full">
                                      <MathExpr expr={val} className="text-xs" />
                                    </span>
                                  </motion.div>
                                ) : (
                                  <motion.input value={val} onChange={e => updateMatrixCell(i, j, e.target.value)}
                                    placeholder={`${i + 1},${j + 1}`}
                                    className={`w-full border-2 rounded-xl px-2 py-2 text-xs text-center bg-white text-slate-900 placeholder-slate-300 focus:outline-none min-h-[40px] ring-1 ${accent}`}
                                    style={MONO}
                                    whileFocus={{ scale: 1.02, boxShadow: "0 0 0 3px rgba(99,102,241,0.15)" }} />
                                )}
                              </motion.div>
                            );
                          })}
                        </motion.div>
                      ))}
                    </div>
                    {/* Color legend */}
                    <div className="flex gap-4 mt-3 pl-12 flex-wrap">
                      {[
                        { dot: "bg-blue-400", label: "A  diagonal" },
                        { dot: "bg-purple-400", label: "B  upper" },
                        { dot: "bg-emerald-400", label: "C  lower" },
                      ].map(({ dot, label }) => (
                        <span key={label} className="flex items-center gap-1.5 text-xs text-slate-400 font-medium">
                          <span className={`w-2.5 h-2.5 rounded-sm ${dot}`} />
                          {label}
                        </span>
                      ))}
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>

              {/* ── Options row ──────────────────────────────────────── */}
              <div className="flex flex-wrap gap-x-6 gap-y-3 mb-6 items-center">
                <label className="flex items-center gap-2.5 cursor-pointer select-none group">
                  <div className="relative">
                    <input type="checkbox" checked={factorizeOption} onChange={e => setFactorizeOption(e.target.checked)}
                      className="peer sr-only" />
                    <div className={`w-5 h-5 rounded-md border-2 flex items-center justify-center transition-all duration-150 ${factorizeOption ? "bg-blue-500 border-blue-500" : "bg-white border-slate-300 group-hover:border-blue-400"}`}>
                      {factorizeOption && <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" strokeWidth={3} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" /></svg>}
                    </div>
                  </div>
                  <span className={`text-sm font-bold transition-colors ${factorizeOption ? "text-blue-600" : "text-slate-600"}`}>Factorize Result</span>
                </label>

                <AnimatePresence>
                  {factorizeOption && (
                    <motion.div initial={{ opacity: 0, x: -8 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -8 }}
                      className="flex items-center gap-2.5">
                      <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Group by:</span>
                      <div className="flex gap-1 p-1 bg-slate-100 rounded-xl">
                        {FACTORIZE_VARS.map(opt => (
                          <motion.button key={opt.value} title={opt.title}
                            onClick={() => setFactorizeVar(opt.value)}
                            className={`relative h-8 px-3.5 rounded-lg text-sm font-bold cursor-pointer overflow-hidden transition-colors ${factorizeVar === opt.value ? "text-white" : "text-slate-500 hover:text-slate-700"}`}
                            whileTap={{ scale: 0.94 }}>
                            {factorizeVar === opt.value && (
                              <motion.span layoutId="fvar-pill" className="absolute inset-0 rounded-lg bg-blue-500 shadow-sm"
                                transition={{ type: "spring", stiffness: 400, damping: 28 }} />
                            )}
                            <span className="relative z-10" style={MONO}>{opt.label}</span>
                          </motion.button>
                        ))}
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>

              {/* ── Compute button ───────────────────────────────────── */}
              <div className="space-y-3">
                <motion.button onClick={computeSymbolic} disabled={isEvaluating}
                  className="relative w-full h-12 overflow-hidden rounded-2xl font-bold text-white cursor-pointer disabled:cursor-not-allowed"
                  style={{
                    background: isEvaluating
                      ? "linear-gradient(135deg,#3b82f6,#2563eb)"
                      : lastResponse
                        ? "linear-gradient(135deg,#3b82f6,#2563eb)"
                        : "linear-gradient(135deg,#3b82f6,#2563eb)",
                    boxShadow: "0 4px 16px rgba(59,130,246,0.35)",
                  }}
                  whileHover={!isEvaluating ? { boxShadow: "0 8px 24px rgba(99,102,241,0.5)", y: -2 } : {}}
                  whileTap={!isEvaluating ? { y: 1 } : {}}>
                  {isEvaluating && (
                    <motion.div className="absolute inset-0 bg-white/10"
                      initial={{ scaleX: 0 }} animate={{ scaleX: computeProgress / 100 }}
                      style={{ transformOrigin: "left" }} transition={{ duration: 0.3, ease: "easeOut" }} />
                  )}
                  <div className="relative flex items-center justify-center gap-2.5">
                    {!isEvaluating && !lastResponse && (<><Zap size={16} /> Compute Symbolic Determinant</>)}
                    {isEvaluating && (<><Loader2 size={16} className="animate-spin" /> {progressLabel} ({computeProgress}%)</>)}
                    {lastResponse && !isEvaluating && (<><Sparkles size={16} /> Recompute</>)}
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
          </motion.div>

          {/* ── Symbolic Matrix Preview (post-compute) ──────────────────── */}
          <AnimatePresence>
            {lastResponse?.symbolicMatrix && (
              <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} className="mb-6">
                <button onClick={() => setShowMatrixPreview(v => !v)}
                  className="w-full flex items-center gap-3 px-6 py-4 bg-white rounded-2xl border-2 border-slate-100 hover:border-slate-200 shadow-sm transition-all cursor-pointer mb-2">
                  <div className={`w-8 h-8 rounded-xl flex items-center justify-center transition-colors ${showMatrixPreview ? "bg-blue-500" : "bg-slate-100"}`}>
                    <Grid3x3 size={15} className={showMatrixPreview ? "text-white" : "text-slate-500"} />
                  </div>
                  <span className="font-bold text-blue-500">Computed Symbolic Matrix</span>
                  <span className="text-xs font-medium text-blue-400 ml-1 bg-slate-100 px-2 py-0.5 rounded-full">
                    {lastResponse.symbolicMatrix.length}×{lastResponse.symbolicMatrix.length}
                    {useTridiagonal ? " · tridiagonal" : ""}
                  </span>
                  <span className="ml-auto text-slate-400">{showMatrixPreview ? <ChevronUp size={16} /> : <ChevronDown size={16} />}</span>
                </button>
                <AnimatePresence>
                  {showMatrixPreview && (
                    <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} exit={{ opacity: 0, height: 0 }} className="overflow-hidden">
                      <SymbolicMatrixPreview matrix={lastResponse.symbolicMatrix} onCellClick={(i, j, expr) => setViewingCell({ i, j, expr })} />
                    </motion.div>
                  )}
                </AnimatePresence>
              </motion.div>
            )}
          </AnimatePresence>

          {/* ── Empty state ──────────────────────────────────────────────── */}
          {!lastResponse && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.3 }}
              className="rounded-3xl border-2 border-dashed border-slate-200 p-12 text-center">
              <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-blue-100 to-purple-100 flex items-center justify-center mx-auto mb-4">
                <FlaskConical size={28} className="text-indigo-500" />
              </div>
              <p className="text-slate-500 text-lg font-medium">Configure your matrix and press <span className="text-indigo-600 font-bold">Compute</span></p>
              <p className="text-slate-400 text-sm mt-1">Results will appear here</p>
            </motion.div>
          )}

          {lastResponse?.error && (
            <div className="bg-red-50 border-2 border-red-200 rounded-2xl p-6">
              <p className="text-red-700 font-bold">{lastResponse.error}</p>
            </div>
          )}

          {/* ── Results ─────────────────────────────────────────────────── */}
          {lastResponse && !lastResponse.error && (
            <div className="space-y-4">

              {/* Expanded Polynomial */}
              {lastResponse.expandedPolynomial && (
                <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
                  className="bg-white rounded-2xl shadow-sm border-2 border-slate-100 overflow-hidden">
                  <div onClick={() => setExpandedOpen(!expandedOpen)} role="button" tabIndex={0}
                    className="w-full px-7 py-5 hover:bg-slate-50 flex items-center justify-between transition-all cursor-pointer">
                    <div className="flex flex-col gap-1.5">
                      <h3 className="font-bold text-slate-900 text-lg">Expanded Polynomial</h3>
                      <div className="flex items-center gap-2 text-xs font-bold" style={MONO}>
                        <span className="bg-blue-100 text-blue-600 px-2 py-0.5 rounded-md">x</span>
                        <span className="bg-purple-100 text-purple-600 px-2 py-0.5 rounded-md">l</span>
                        <span className="bg-orange-100 text-orange-600 px-2 py-0.5 rounded-md">d</span>
                        <span className="bg-emerald-100 text-emerald-600 px-2 py-0.5 rounded-md">λ</span>
                        <span className="bg-rose-100 text-rose-500 px-2 py-0.5 rounded-md">other</span>
                      </div>
                    </div>
                    <div className="flex items-center gap-2.5">
                      <button onClick={e => { e.stopPropagation(); copyToClipboard(lastResponse.expandedPolynomial!, "expanded"); }}
                        className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 rounded-xl text-xs font-bold text-slate-600 transition-all">
                        {copiedSection === "expanded" ? <><Check size={13} /> Copied</> : <><Copy size={13} /> Copy</>}
                      </button>
                      <div className="text-slate-400">{expandedOpen ? <ChevronUp size={18} /> : <ChevronDown size={18} />}</div>
                    </div>
                  </div>
                  <AnimatePresence>
                    {expandedOpen && (
                      <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: "auto", opacity: 1 }} exit={{ height: 0, opacity: 0 }} transition={{ duration: 0.2 }} className="overflow-hidden">
                        <div className="px-7 pb-6 pt-1 overflow-x-auto border-t-2 border-slate-100">
                          <pre className="text-sm whitespace-pre-wrap break-words leading-relaxed pt-4">
                            <MathExpr expr={lastResponse.expandedPolynomial!} />
                          </pre>
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </motion.div>
              )}

              {/* Factored Form */}
              {lastResponse.factorized && lastResponse.factorized.length > 0 && (
                <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.05 }}
                  className="bg-white rounded-2xl shadow-sm border-2 border-slate-100 overflow-hidden">
                  <button onClick={() => setFactorizedOpen(!factorizedOpen)}
                    className="w-full px-7 py-5 hover:bg-slate-50 flex items-center justify-between transition-all cursor-pointer">
                    <div className="flex items-center gap-3">
                      <h3 className="font-bold text-slate-900 text-lg">Factored Form</h3>
                      <span className="text-xs bg-slate-100 text-blue-500 font-bold px-2.5 py-1 rounded-full">
                        {lastResponse.factorized.length} groups
                      </span>
                      <span className="text-xs text-slate-400">
                        by <span className="font-bold text-blue-600" style={MONO}>{factorizeVar === "lam" ? "λ" : factorizeVar}</span>
                      </span>
                    </div>
                    <div className="text-slate-400">{factorizedOpen ? <ChevronUp size={18} /> : <ChevronDown size={18} />}</div>
                  </button>
                  <AnimatePresence>
                    {factorizedOpen && (
                      <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: "auto", opacity: 1 }} exit={{ height: 0, opacity: 0 }} transition={{ duration: 0.2 }} className="overflow-hidden">
                        <div className="px-7 pb-6 pt-1 space-y-3 border-t-2 border-slate-100">
                          {lastResponse.factorized.map((group, idx) => (
                            <div key={idx} className="border-2 border-slate-100 rounded-2xl overflow-hidden mt-3">
                              <div onClick={() => toggleTerm(idx)} role="button" tabIndex={0}
                                className="w-full px-5 py-3.5 bg-slate-50 hover:bg-slate-100 flex items-center justify-between transition-all cursor-pointer">
                                <span className="font-semibold text-slate-800 text-sm flex items-center gap-2">
                                  Terms with
                                  <code className="bg-white px-2.5 py-1 rounded-lg border border-slate-200 text-xs shadow-sm">
                                    <MathExpr expr={group.power === "" ? "constant" : group.power} />
                                  </code>
                                  <span className="text-slate-400 font-normal">({group.terms.length})</span>
                                </span>
                                <div className="flex items-center gap-2">
                                  <button onClick={e => { e.stopPropagation(); copyToClipboard(group.terms.join(" + "), `factorized-${idx}`); }}
                                    className="p-1.5 bg-white hover:bg-slate-200 rounded-lg border border-slate-200 text-slate-500 transition-all">
                                    {copiedSection === `factorized-${idx}` ? <Check size={13} /> : <Copy size={13} />}
                                  </button>
                                  <div className="text-slate-400">{expandedTerms.has(idx) ? <ChevronUp size={16} /> : <ChevronDown size={16} />}</div>
                                </div>
                              </div>
                              <AnimatePresence>
                                {expandedTerms.has(idx) && (
                                  <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: "auto", opacity: 1 }} exit={{ height: 0, opacity: 0 }} transition={{ duration: 0.15 }} className="overflow-hidden">
                                    <div className="p-4 space-y-2 bg-slate-50/50 border-t-2 border-slate-100">
                                      {group.terms.map((term, i) => (
                                        <div key={i} className="p-3 bg-white rounded-xl border-2 border-slate-100 break-words">
                                          <MathExpr expr={term} className="text-sm" />
                                        </div>
                                      ))}
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
                </motion.div>
              )}

              {/* Numeric Evaluation */}
              {(showNumeric || isPureNumeric) && (
                <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}
                  className="bg-white rounded-2xl shadow-sm border-2 border-slate-100 p-7">
                  <h3 className="font-bold text-slate-900 text-lg mb-5">Numeric Evaluation</h3>
                  {numericError && (
                    <div className="mb-5 p-4 bg-red-50 border-2 border-red-200 rounded-2xl flex gap-3">
                      <AlertCircle size={18} className="text-red-500 shrink-0 mt-0.5" />
                      <div><p className="font-bold text-red-700 text-sm">Evaluation Error</p><p className="text-xs text-red-600 mt-0.5">{numericError}</p></div>
                    </div>
                  )}
                  {detectedVars.length > 0 && (
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-5">
                      {detectedVars.map(v => (
                        <div key={v}>
                          <label
                            className="block text-xs font-bold text-slate-500 mb-1.5 uppercase tracking-widest"
                            style={MONO}
                          >
                            {v === "lambda" ? "λ" : v}
                          </label>

                          <input
                            type="number"
                            value={numericInputs[v as keyof typeof numericInputs] ?? ""}
                            onChange={e =>
                              setNumericInputs(prev => ({ ...prev, [v]: e.target.value }))
                            }
                            placeholder="0"
                            className="w-full border-2 border-slate-200 rounded-xl px-3 py-2.5 text-sm font-mono focus:outline-none focus:border-indigo-400 focus:ring-3 focus:ring-indigo-100 transition-all"
                            style={MONO}
                          />
                        </div>
                      ))}
                    </div>
                  )}
                  {detectedVars.length > 0 && (
                    <button onClick={computeNumeric} disabled={isEvaluating}
                      className="w-full bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-600 hover:to-teal-600 disabled:opacity-60 text-white font-bold py-3 rounded-2xl transition-all shadow-md hover:shadow-lg mb-5 cursor-pointer"
                      style={{ boxShadow: "0 4px 16px rgba(16,185,129,0.3)" }}>
                      {isEvaluating ? "⟳ Evaluating..." : "Evaluate Numerically"}
                    </button>
                  )}
                  {(lastResponse.numericFromSymbolic != null || lastResponse.numericFromLU != null) && (
                    <div className="grid md:grid-cols-2 gap-4">
                      {lastResponse.numericFromSymbolic != null && (
                        <div className="bg-gradient-to-br from-blue-50 to-indigo-50 border-2 border-blue-200 rounded-2xl p-5">
                          <h4 className="text-xs font-bold text-blue-500 uppercase tracking-widest mb-2">Symbolic Substitution</h4>
                          <p className="text-xl font-bold text-blue-900" style={MONO}>{lastResponse.numericFromSymbolic.toFixed(8)}</p>
                        </div>
                      )}
                      {lastResponse.numericFromLU != null && (
                        <div className="bg-gradient-to-br from-emerald-50 to-teal-50 border-2 border-emerald-200 rounded-2xl p-5">
                          <h4 className="text-xs font-bold text-emerald-600 uppercase tracking-widest mb-2">Numeric LU</h4>
                          <p className="text-xl font-bold text-emerald-900" style={MONO}>{lastResponse.numericFromLU.toFixed(8)}</p>
                        </div>
                      )}
                    </div>
                  )}
                </motion.div>
              )}

              {/* Detected Variables */}
              {lastResponse.detectedVariables && lastResponse.detectedVariables.length > 0 && (
                <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.15 }}
                  className="bg-white rounded-2xl shadow-sm border-2 border-slate-100 p-6">
                  <h3 className="font-bold text-slate-900 mb-3">Detected Variables</h3>
                  <div className="flex flex-wrap gap-2">
                    {lastResponse.detectedVariables.map((v, i) => (
                      <span key={i} className="bg-indigo-50 border-2 border-indigo-100 text-indigo-700 px-3 py-1.5 rounded-xl text-xs font-bold" style={MONO}>{v}</span>
                    ))}
                  </div>
                </motion.div>
              )}

              {/* LU Steps */}
              {lastResponse.luSteps && lastResponse.luSteps.length > 0 && (
                <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }}
                  className="bg-white rounded-2xl shadow-sm border-2 border-slate-100 p-6">
                  <h3 className="font-bold text-slate-900 mb-3">LU Steps ({lastResponse.luSteps.length})</h3>
                  <div className="space-y-2 max-h-80 overflow-y-auto">
                    {lastResponse.luSteps.map((step, i) => (
                      <div key={i} className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                        <p className="text-xs text-slate-600 break-words" style={MONO}>{step}</p>
                      </div>
                    ))}
                  </div>
                </motion.div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* ── Cell View Modal ──────────────────────────────────────────────────── */}
      <AnimatePresence>
        {viewingCell && (
          <>
            <motion.div className="fixed inset-0" style={{ zIndex: 9998, backgroundColor: "rgba(15,23,42,0.65)", backdropFilter: "blur(12px)" }}
              initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setViewingCell(null)} />
            <div className="fixed inset-0 flex items-center justify-center p-6 pointer-events-none" style={{ zIndex: 9999 }}>
              <motion.div className="bg-white rounded-3xl shadow-2xl border-2 border-slate-100 w-full max-w-2xl pointer-events-auto overflow-hidden"
                initial={{ opacity: 0, scale: 0.9, y: 20 }} animate={{ opacity: 1, scale: 1, y: 0 }} exit={{ opacity: 0, scale: 0.9, y: 20 }}
                transition={{ type: "spring", stiffness: 400, damping: 30 }}>

                {/* Header */}
                <div className="px-7 pt-7 pb-5 border-b-2 border-slate-100 flex items-start justify-between">
                  <div>
                    <span className="text-xs font-bold text-blue-400 uppercase tracking-widest">Symbolic Matrix</span>
                    <h3 className="text-xl font-bold text-slate-900 mt-0.5">
                      Cell M[{viewingCell.i + 1},{viewingCell.j + 1}]
                    </h3>
                    <div className="mt-2 flex items-center gap-2">
                      {viewingCell.j === viewingCell.i && (
                        <span className="text-xs font-bold bg-blue-100 text-blue-700 px-2.5 py-1 rounded-full">
                          A<sub>{viewingCell.i + 1}</sub> — diagonal
                        </span>
                      )}
                      {viewingCell.j === viewingCell.i + 1 && (
                        <span className="text-xs font-bold bg-purple-100 text-purple-700 px-2.5 py-1 rounded-full">
                          B<sub>{viewingCell.i + 1}</sub> — upper diagonal
                        </span>
                      )}
                      {viewingCell.j === viewingCell.i - 1 && (
                        <span className="text-xs font-bold bg-emerald-100 text-emerald-700 px-2.5 py-1 rounded-full">
                          C<sub>{viewingCell.i + 1}</sub> — lower diagonal
                        </span>
                      )}
                    </div>
                  </div>
                  <motion.button onClick={() => setViewingCell(null)}
                    className="w-9 h-9 rounded-xl bg-slate-100 flex items-center justify-center text-slate-500 cursor-pointer mt-1"
                    whileHover={{ scale: 1.08, backgroundColor: "#e2e8f0" }} whileTap={{ scale: 0.93 }}>
                    <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                      <path d="M1 1L11 11M11 1L1 11" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                    </svg>
                  </motion.button>
                </div>

                {/* Body */}
                <div className="px-7 py-6 space-y-4">
                  {/* Coloured expression */}
                  <div>
                    <p className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">Expression</p>
                    <div className="p-5 bg-slate-50 rounded-2xl border-2 border-slate-100 overflow-x-auto">
                      <p className="text-sm leading-relaxed whitespace-pre-wrap break-words" style={MONO}>
                        <MathExpr expr={viewingCell.expr} />
                      </p>
                    </div>
                  </div>

                  {/* Raw copyable text */}
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <p className="text-xs font-bold text-slate-400 uppercase tracking-widest">Raw text</p>
                      <motion.button
                        onClick={() => copyToClipboard(viewingCell.expr, "view-cell")}
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer border-2"
                        style={copiedSection === "view-cell"
                          ? { background: "#ffffff", borderColor: "#0091ff", color: "#0091ff" }
                          : { background: "#f8fafc", borderColor: "#e2e8f0", color: "#64748b" }}
                        whileHover={{ scale: 1.03 }} whileTap={{ scale: 0.97 }}>
                        {copiedSection === "view-cell"
                          ? <><Check size={12} /> Copied!</>
                          : <><Copy size={12} /> Copy expression</>}
                      </motion.button>
                    </div>
                    <div className="p-4 bg-white border-2 border-slate-200 rounded-xl overflow-x-auto">
                      <p className="text-xs text-blue-500 whitespace-pre-wrap break-all" style={MONO}>{viewingCell.expr}</p>
                    </div>
                  </div>
                </div>

                {/* Footer */}
                <div className="px-7 pb-7 flex justify-end">
                  <motion.button onClick={() => setViewingCell(null)}
                    className="px-6 py-2.5 rounded-xl text-sm bg-blue-500 font-bold text-white cursor-pointer"
                    whileHover={{ scale: 1.03, boxShadow: "0 6px 16px rgba(99,102,241,0.5)" }}
                    whileTap={{ scale: 0.97 }}>Close</motion.button>
                </div>
              </motion.div>
            </div>
          </>
        )}
      </AnimatePresence>

      {/* ── Cell Edit Modal ──────────────────────────────────────────────────── */}
      <AnimatePresence>
        {editingCell && (
          <>
            <motion.div className="fixed inset-0" style={{ zIndex: 9998, backgroundColor: "rgba(15,23,42,0.6)", backdropFilter: "blur(12px)" }}
              initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setEditingCell(null)} />
            <div className="fixed inset-0 flex items-center justify-center p-6 pointer-events-none" style={{ zIndex: 9999 }}>
              <motion.div className="bg-white rounded-3xl shadow-2xl border-2 border-slate-100 w-full max-w-lg pointer-events-auto overflow-hidden"
                initial={{ opacity: 0, scale: 0.9, y: 20 }} animate={{ opacity: 1, scale: 1, y: 0 }} exit={{ opacity: 0, scale: 0.9, y: 20 }}
                transition={{ type: "spring", stiffness: 400, damping: 30 }}>
                <div className="px-7 pt-7 pb-5 border-b-2 border-slate-100">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="text-xs font-bold text-indigo-400 uppercase tracking-widest">Matrix Cell</span>
                      <h3 className="text-xl font-bold text-slate-900 mt-0.5">Edit M[{editingCell.i + 1},{editingCell.j + 1}]</h3>
                    </div>
                    <motion.button onClick={() => setEditingCell(null)}
                      className="w-9 h-9 rounded-xl bg-slate-100 flex items-center justify-center text-slate-500 cursor-pointer"
                      whileHover={{ scale: 1.08, backgroundColor: "#e2e8f0" }} whileTap={{ scale: 0.93 }}>
                      <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                        <path d="M1 1L11 11M11 1L1 11" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                      </svg>
                    </motion.button>
                  </div>
                  {matrixValues[editingCell.i][editingCell.j] && (
                    <div className="mt-4 px-4 py-3 bg-slate-50 rounded-2xl border-2 border-slate-100">
                      <p className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Current</p>
                      <MathExpr expr={matrixValues[editingCell.i][editingCell.j]} className="text-sm" />
                    </div>
                  )}
                </div>
                <div className="px-7 py-6">
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-2.5">New expression</label>
                  <motion.textarea value={editingValue} onChange={e => setEditingValue(e.target.value)}
                    onKeyDown={e => { if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) confirmCellEdit(); if (e.key === "Escape") setEditingCell(null); }}
                    placeholder={`M[${editingCell.i + 1},${editingCell.j + 1}]`} rows={3} autoFocus
                    className="w-full border-2 border-slate-200 rounded-2xl px-4 py-3 text-sm text-slate-900 bg-slate-50 placeholder-slate-300 focus:outline-none resize-none focus:border-indigo-400 focus:ring-3 focus:ring-indigo-100 transition-all"
                    style={MONO} />
                  <p className="text-xs text-slate-400 mt-2">Use <code className="bg-slate-100 px-1.5 rounded-md">*</code> for multiplication, <code className="bg-slate-100 px-1.5 rounded-md">^</code> for powers.</p>
                </div>
                <div className="px-7 pb-7 flex gap-2.5 justify-end">
                  <motion.button onClick={() => setEditingCell(null)}
                    className="px-5 py-2.5 rounded-xl text-sm font-bold text-slate-600 bg-slate-100 cursor-pointer"
                    whileHover={{ backgroundColor: "#e2e8f0" }} whileTap={{ scale: 0.97 }}>Cancel</motion.button>
                  <motion.button onClick={confirmCellEdit}
                    className="px-6 py-2.5 rounded-xl text-sm font-bold text-white cursor-pointer bg-blue-500"
                    whileHover={{ scale: 1.03, boxShadow: "0 6px 16px rgba(99,102,241,0.5)" }}
                    whileTap={{ scale: 0.97 }}>Confirm</motion.button>
                </div>
              </motion.div>
            </div>
          </>
        )}
      </AnimatePresence>
    </>
  );
}