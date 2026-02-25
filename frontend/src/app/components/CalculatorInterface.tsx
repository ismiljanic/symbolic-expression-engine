"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "motion/react";
import { Button } from "./ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "./ui/tabs";
import { ChevronDown, ChevronUp, Copy, Check, AlertCircle } from "lucide-react";

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
    ["0", "0", "-6", "(x_l-1)*x_l*(x_l+1)+((x_l-1)+(x_l+1))*(d/2) - lambda"]
  ],
  5: [
    ["10*(x_l-3) + 6*(x_l+1) - lambda", "6*t_l", "0", "0", "(t_l-3)*(t_l-2)*(t_l-1)*t_l"],
    ["-4", "10*(x_l-3) - 1*(x_l) - lambda", "3*(t_l-1)", "0", "-1*(t_l-3)*(t_l-2)*(t_l-1)*(x_l+1)"],
    ["0", "-7", "10*(x_l-3) - 6*(x_l-1) - lambda", "t_l-2", "(t_l-3)*(t_l-2)*((x_l)*(x_l+1)+(d/3))"],
    ["0", "0", "-9", "10*(x_l-3) - 9*(x_l-2) - lambda", "-1*(t_l-3)*((x_l-1)*x_l*(x_l+1)+((x_l-1)+(x_l+1))*(d/2))"],
    ["0", "0", "0", "-10", "(x_l-2)*(x_l-1)*x_l*(x_l+1)+(((5*(x_l+1))+(3*(x_l-3)))*((x_l-2)+x_l)+(6*(x_l-1)*x_l)-2*(x_l-3)*(x_l-2)+2*d)*(d/10) - lambda"]
  ]
};

export function CalculatorInterface() {
  const [matrixSize, setMatrixSize] = useState(2);
  const [matrixValues, setMatrixValues] = useState(
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

  // Separate toggles for expanded and factored forms
  const [expandedOpen, setExpandedOpen] = useState(true);
  const [factorizedOpen, setFactorizedOpen] = useState(true);
  const [expandedTerms, setExpandedTerms] = useState<Set<number>>(new Set());
  const [showFactorizedTerms, setShowFactorizedTerms] = useState<Set<number>>(new Set());
  const updateMatrixCell = (i: number, j: number, value: string) => {
    const newMatrix = matrixValues.map(row => [...row]);
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
      setMatrixValues(PREDEFINED_MATRICES[matrixSize].map(row => [...row]));
      setShowNumeric(false);
    }
  };

  const copyToClipboard = (text: string, section: string) => {
    navigator.clipboard.writeText(text);
    setCopiedSection(section);
    setTimeout(() => setCopiedSection(null), 2000);
  };

  const computeSymbolic = async () => {
    const payload: any = {
      matrix: matrixValues,
      factorize: factorizeOption,
      detailedLU: detailedLUOption
    };

    try {
      const res = await fetch("http://localhost:8080/api/determinant", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      const data: DeterminantResponse = await res.json();
      if (res.ok) {
        setLastResponse(data);
        setShowNumeric((data.detectedVariables?.length ?? 0) > 0);
        setExpandedTerms(new Set());
        setShowFactorizedTerms(new Set());
        setNumericError(null);
        console.log("API response:", data.factorized);
      } else {
        setLastResponse({ error: data.error || `HTTP ${res.status}` });
        setShowNumeric(false);
      }
    } catch (err: any) {
      setLastResponse({ error: "Network error: " + err.message });
      setShowNumeric(false);
    }
  };

  const computeNumeric = async () => {
    setNumericError(null);
    setIsEvaluating(true);

    const payload: any = {
      matrix: matrixValues,
      xValue: parseFloat(numericInputs.x),
      lValue: parseFloat(numericInputs.l),
      dValue: parseFloat(numericInputs.d) || 0,
      lambdaValue: parseFloat(numericInputs.lambda) || 0,
      factorize: factorizeOption,
      detailedLU: detailedLUOption
    };

    try {
      const res = await fetch("http://localhost:8080/api/determinant", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      if (!res.ok) {
        const errorText = await res.text();
        setNumericError(`Server error: ${res.status} - ${errorText}`);
        setIsEvaluating(false);
        return;
      }

      const data: DeterminantResponse = await res.json();

      if (data.error) {
        setNumericError(data.error);
      } else {
        setLastResponse(prev => ({ ...prev, ...data }));
        setNumericError(null);
      }
    } catch (err: any) {
      setNumericError("Network error: " + err.message);
    } finally {
      setIsEvaluating(false);
    }
  };

  const toggleTerm = (idx: number) => {
    setExpandedTerms(prev => {
      const next = new Set(prev);
      next.has(idx) ? next.delete(idx) : next.add(idx);
      return next;
    });
  };

  const toggleFactorizedTerms = (idx: number) => {
    setShowFactorizedTerms(prev => {
      const next = new Set(prev);
      next.has(idx) ? next.delete(idx) : next.add(idx);
      return next;
    });
  };

  const escHtml = (s?: string) =>
    String(s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-50 p-6">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-4xl font-bold text-slate-900 mb-2">Determinant Analysis</h1>
          <p className="text-slate-600">Symbolic computation & polynomial factorization</p>
        </div>

        {/* Matrix Input Section */}
        <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6 mb-6">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">Matrix Input ({matrixSize}×{matrixSize})</h2>

          {/* Size Selector */}
          <div className="flex gap-2 mb-6">
            {[2, 3, 4, 5, 6].map(n => (
              <button
                key={n}
                onClick={() => handleSizeChange(n)}
                className={`px-4 py-2 rounded font-medium transition-all ${matrixSize === n
                  ? "bg-blue-600 text-white shadow-sm"
                  : "bg-slate-100 text-slate-900 hover:bg-slate-200"
                  }`}
              >
                {n}×{n}
              </button>
            ))}
            <button
              onClick={loadPredefined}
              className="px-4 py-2 rounded font-medium bg-amber-100 text-amber-900 hover:bg-amber-200 transition-all"
            >
              Load Predefined
            </button>
          </div>

          {/* Matrix Grid */}
          <div className="grid gap-2 mb-6" style={{ gridTemplateColumns: `repeat(${matrixSize}, minmax(0, 1fr))` }}>
            {matrixValues.flatMap((row, i) =>
              row.map((val, j) => (
                <input
                  key={`${i}-${j}`}
                  value={val}
                  onChange={e => updateMatrixCell(i, j, e.target.value)}
                  placeholder={`${i + 1},${j + 1}`}
                  className="border border-slate-300 rounded px-3 py-2 text-sm text-center focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              ))
            )}
          </div>

          {/* Options */}
          <div className="flex gap-6 mb-6">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={factorizeOption}
                onChange={e => setFactorizeOption(e.target.checked)}
                className="w-4 h-4 border border-slate-300 rounded focus:ring-2 focus:ring-blue-500"
              />
              <span className="text-sm font-medium text-slate-700">Factorize Result</span>
            </label>
            {/* <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={detailedLUOption}
                onChange={e => setDetailedLUOption(e.target.checked)}
                className="w-4 h-4 border border-slate-300 rounded focus:ring-2 focus:ring-blue-500"
              />
              <span className="text-sm font-medium text-slate-700">Show LU Steps</span>
            </label> */}
          </div>

          <button
            onClick={computeSymbolic}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 rounded-lg transition-all shadow-sm"
          >
            ▶ Compute Symbolic
          </button>
        </div>

        {/* Results Section */}
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
            {/* Obsolete for now, but keeping the code for potential future use: */}
            {/* Factorized Expression - NEW SECTION
            {lastResponse.factorized && lastResponse.factorized.length > 0 && (
              <div className="bg-white rounded-lg shadow-sm border border-slate-200 overflow-hidden">
                <div className="bg-gradient-to-r from-blue-50 to-indigo-50 px-6 py-4 border-b border-slate-200">
                  <h3 className="font-semibold text-slate-900 text-lg">Factorized Expression</h3>
                  <p className="text-sm text-slate-600 mt-1">Grouped by λ power, then x</p>
                </div>
                <div className="p-6">
                  <div className="space-y-3">
                    {lastResponse.factorized.map((group, idx) => (
                      <div key={idx} className="border border-slate-200 rounded-lg bg-slate-50 overflow-hidden">
                        <button
                          onClick={() => toggleFactorizedTerms(idx)}
                          className="w-full flex items-center justify-between p-4 border-b border-slate-200 hover:bg-slate-100 transition-colors"
                        >
                          <span className="font-semibold text-slate-900">
                            Terms with <code className="bg-white px-2 py-1 rounded text-blue-600 font-mono text-sm ml-1">{group.power}</code>
                          </span>
                          <div className="flex items-center gap-2">
                            <span className="text-xs font-medium text-slate-700 bg-white px-2 py-1 rounded border border-slate-200">
                              {group.terms.length} {group.terms.length === 1 ? 'term' : 'terms'}
                            </span>
                            {showFactorizedTerms.has(idx) ? (
                              <ChevronUp size={18} className="text-slate-600" />
                            ) : (
                              <ChevronDown size={18} className="text-slate-600" />
                            )}
                          </div>
                        </button>
                        
                        <AnimatePresence>
                          {showFactorizedTerms.has(idx) && (
                            <motion.div
                              initial={{ height: 0, opacity: 0 }}
                              animate={{ height: "auto", opacity: 1 }}
                              exit={{ height: 0, opacity: 0 }}
                              transition={{ duration: 0.2 }}
                              className="overflow-hidden"
                            >
                              <div className="divide-y divide-slate-200">
                                {group.terms.map((term, i) => (
                                  <div key={i} className="flex gap-3 p-4 hover:bg-white transition-colors">
                                    <div className="w-8 text-center text-slate-500 font-mono text-sm flex-shrink-0 pt-1">
                                      {i > 0 ? '+' : ''}
                                    </div>
                                    <div className="font-mono text-sm text-slate-700 break-words flex-1 pt-1">
                                      {escHtml(term)}
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </motion.div>
                          )}
                        </AnimatePresence>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )} */}

            {/* Obsolete for now, but keeping the code for potential future use: */}

            {/* Symbolic LU Determinant
            {lastResponse.symbolicDeterminant && (
              <div className="bg-white rounded-lg shadow-sm border border-slate-200 overflow-hidden">
                <div className="bg-slate-50 px-6 py-4 border-b border-slate-200 flex items-center justify-between">
                  <h3 className="font-semibold text-slate-900">Symbolic LU Determinant</h3>
                  <button
                    onClick={() => copyToClipboard(lastResponse.symbolicDeterminant!, "symbolic")}
                    className="flex items-center gap-2 px-3 py-1 bg-slate-200 hover:bg-slate-300 rounded text-sm font-medium text-slate-900 transition-all"
                  >
                    {copiedSection === "symbolic" ? (
                      <>
                        <Check size={16} /> Copied
                      </>
                    ) : (
                      <>
                        <Copy size={16} /> Copy
                      </>
                    )}
                  </button>
                </div>
                <div className="p-6 overflow-x-auto">
                  <pre className="font-mono text-sm text-slate-700 whitespace-pre-wrap break-words">
                    {escHtml(lastResponse.symbolicDeterminant)}
                  </pre>
                </div>
              </div>
            )} */}

            {/* Expanded Polynomial with Toggle */}
            {lastResponse.expandedPolynomial && (
              <div className="bg-white rounded-lg shadow-sm border border-slate-200 overflow-hidden">
                <button
                  onClick={() => setExpandedOpen(!expandedOpen)}
                  className="w-full px-6 py-4 bg-slate-50 hover:bg-slate-100 border-b border-slate-200 flex items-center justify-between transition-all"
                >
                  <h3 className="font-semibold text-slate-900">Expanded Polynomial</h3>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={e => {
                        e.stopPropagation();
                        copyToClipboard(lastResponse.expandedPolynomial!, "expanded");
                      }}
                      className="flex items-center gap-2 px-3 py-1 bg-slate-200 hover:bg-slate-300 rounded text-sm font-medium text-slate-900 transition-all"
                    >
                      {copiedSection === "expanded" ? (
                        <>
                          <Check size={16} /> Copied
                        </>
                      ) : (
                        <>
                          <Copy size={16} /> Copy
                        </>
                      )}
                    </button>
                    {expandedOpen ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
                  </div>
                </button>
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
                        <pre className="font-mono text-sm text-slate-700 whitespace-pre-wrap break-words">
                          {escHtml(lastResponse.expandedPolynomial)}
                        </pre>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            )}

            {/* Factorized Form with Toggle */}
            {lastResponse.factorized && lastResponse.factorized.length > 0 && (
              <div className="bg-white rounded-lg shadow-sm border border-slate-200 overflow-hidden">
                <button
                  onClick={() => setFactorizedOpen(!factorizedOpen)}
                  className="w-full px-6 py-4 bg-slate-50 hover:bg-slate-100 border-b border-slate-200 flex items-center justify-between transition-all"
                >
                  <h3 className="font-semibold text-slate-900">
                    Factored Form — {lastResponse.factorized.length} groups
                  </h3>
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
                            {/* Group Header */}
                            <button
                              onClick={() => toggleTerm(idx)}
                              className="w-full px-4 py-3 bg-slate-50 hover:bg-slate-100 flex items-center justify-between transition-all"
                            >
                              <span className="font-medium text-slate-900">
                                Terms with <code className="bg-slate-100 px-2 py-1 rounded text-blue-600">{group.power == '' ? "Constant" : group.power}</code>
                                <span className="text-slate-600 ml-2">({group.terms.length} {group.terms.length === 1 ? 'term' : 'terms'})</span>
                              </span>
                              <div className="flex items-center gap-2">
                                <button
                                  onClick={e => {
                                    e.stopPropagation();
                                    copyToClipboard(group.terms.join(" + "), `factorized-${idx}`);
                                  }}
                                  className="flex items-center gap-1 px-2 py-1 bg-slate-200 hover:bg-slate-300 rounded text-xs font-medium text-slate-900 transition-all"
                                >
                                  {copiedSection === `factorized-${idx}` ? <Check size={14} /> : <Copy size={14} />}
                                </button>
                                {expandedTerms.has(idx) ? (
                                  <ChevronUp size={18} />
                                ) : (
                                  <ChevronDown size={18} />
                                )}
                              </div>
                            </button>

                            {/* Expanded Terms List */}
                            <AnimatePresence>
                              {expandedTerms.has(idx) && (
                                <motion.div
                                  initial={{ height: 0, opacity: 0 }}
                                  animate={{ height: "auto", opacity: 1 }}
                                  exit={{ height: 0, opacity: 0 }}
                                  transition={{ duration: 0.2 }}
                                  className="overflow-hidden bg-slate-50 border-t border-slate-200"
                                >
                                  {/* Factorized Terms Display */}
                                  <motion.div
                                    initial={{ height: 0, opacity: 0 }}
                                    animate={{ height: "auto", opacity: 1 }}
                                    exit={{ height: 0, opacity: 0 }}
                                    transition={{ duration: 0.2 }}
                                    className="overflow-hidden bg-blue-50 border-t border-slate-200"
                                  >
                                    <div className="p-4">
                                      <p className="text-xs font-semibold text-blue-900 mb-3">FACTORIZED FORM</p>
                                      <div className="space-y-2">
                                        {group.terms.map((term, i) => (
                                          <div
                                            key={i}
                                            className="font-mono text-sm text-blue-800 p-3 bg-white rounded border border-blue-200 break-words"
                                          >
                                            {escHtml(term)}
                                          </div>
                                        ))}
                                      </div>
                                    </div>
                                  </motion.div>
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
            {showNumeric && (
              <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
                <h3 className="text-lg font-semibold text-slate-900 mb-4">Numeric Evaluation</h3>

                {/* Error Alert */}
                {numericError && (
                  <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex gap-3">
                    <AlertCircle size={20} className="text-red-600 flex-shrink-0 mt-0.5" />
                    <div>
                      <p className="font-semibold text-red-900">Evaluation Error</p>
                      <p className="text-sm text-red-800 mt-1">{numericError}</p>
                    </div>
                  </div>
                )}

                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                  {["x", "l", "d", "lambda"].map(k => (
                    <div key={k}>
                      <label className="block text-sm font-medium text-slate-700 mb-2 capitalize">{k}</label>
                      <input
                        type="number"
                        value={numericInputs[k as keyof typeof numericInputs]}
                        onChange={e => setNumericInputs(prev => ({ ...prev, [k]: e.target.value }))}
                        placeholder="0"
                        className="w-full border border-slate-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                  ))}
                </div>

                <button
                  onClick={computeNumeric}
                  disabled={isEvaluating}
                  className="w-full bg-green-600 hover:bg-green-700 disabled:bg-green-400 text-white font-semibold py-3 rounded-lg transition-all shadow-sm mb-6"
                >
                  {isEvaluating ? "⟳ Evaluating..." : "⟳ Evaluate Numerically"}
                </button>

                {(lastResponse.numericFromSymbolic != null || lastResponse.numericFromLU != null) && (
                  <div className="grid md:grid-cols-2 gap-4">
                    {lastResponse.numericFromSymbolic != null && (
                      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                        <h4 className="font-semibold text-blue-900 mb-2">Symbolic Substitution</h4>
                        <p className="font-mono text-lg text-blue-700">
                          {lastResponse.numericFromSymbolic.toFixed(8)}
                        </p>
                      </div>
                    )}
                    {lastResponse.numericFromLU != null && (
                      <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                        <h4 className="font-semibold text-green-900 mb-2">Numeric LU</h4>
                        <p className="font-mono text-lg text-green-700">
                          {lastResponse.numericFromLU.toFixed(8)}
                        </p>
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
                    <span
                      key={i}
                      className="inline-block bg-blue-100 text-blue-900 px-3 py-1 rounded-full font-mono text-sm"
                    >
                      {v}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* LU Steps */}
            {lastResponse.luSteps && lastResponse.luSteps.length > 0 && (
              <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
                <h3 className="text-lg font-semibold text-slate-900 mb-4">
                  LU Steps ({lastResponse.luSteps.length})
                </h3>
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
  );
}