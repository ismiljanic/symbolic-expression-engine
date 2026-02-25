import { motion } from "motion/react";
import { Calculator, Sigma, CheckCircle2, Layers, Network, Sparkles } from "lucide-react";
import { MatrixVisualization } from "./components/MatrixVisualization";
import { FeatureCard } from "./components/FeatureCard";
import { ProcessFlow } from "./components/ProcessFlow";
import { CodeExample } from "./components/CodeExample";
import { CalculatorInterface } from "./components/CalculatorInterface";
import { Button } from "./components/ui/button";
import { useState } from "react";

export default function App() {
  const [showCalculator, setShowCalculator] = useState(false);

  return (
    <div className="min-h-screen bg-gradient-to-b from-white via-gray-50 to-white">
      {/* Header */}
      <header className="border-b border-gray-100 bg-white/80 backdrop-blur-sm sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-blue-700 rounded-xl flex items-center justify-center">
              <Calculator className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="text-lg font-medium">SymbolicDet</h1>
              <p className="text-xs text-gray-500">Computer Algebra System</p>
            </div>
          </div>
          <nav className="flex items-center gap-6">
            <a href="#features" className="text-sm text-gray-600 hover:text-gray-900 transition-colors">
              Features
            </a>
            <a href="#how-it-works" className="text-sm text-gray-600 hover:text-gray-900 transition-colors">
              How it Works
            </a>
            <Button 
              variant="default" 
              className="bg-blue-600 hover:bg-blue-700"
              onClick={() => setShowCalculator(!showCalculator)}
            >
              {showCalculator ? "Back to Home" : "Launch Calculator"}
            </Button>
          </nav>
        </div>
      </header>

      {showCalculator ? (
        // Calculator Interface
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.5 }}
        >
          <div className="max-w-7xl mx-auto px-6 py-12">
            <div className="text-center mb-12">
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
              >
                <h2 className="text-4xl mb-3">Symbolic Determinant Calculator</h2>
                <p className="text-gray-600 text-lg">
                  View expanded and factorized expressions for 4×4 tridiagonal matrix
                </p>
              </motion.div>
            </div>
            <CalculatorInterface />
          </div>
        </motion.div>
      ) : (
        // Original Landing Page
        <>
          {/* Hero Section */}
          <section className="max-w-7xl mx-auto px-6 pt-20 pb-24">
            <div className="text-center mb-16">
              <motion.div
                className="inline-flex items-center gap-2 px-4 py-2 bg-blue-50 rounded-full mb-6 border border-blue-100"
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
              >
                <Sparkles className="w-4 h-4 text-blue-600" />
                <span className="text-sm text-blue-700 font-medium">Symbolic Computation Engine</span>
              </motion.div>
              
              <motion.h1
                className="text-6xl mb-6 bg-gradient-to-r from-gray-900 via-gray-800 to-gray-900 bg-clip-text text-transparent"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, delay: 0.1 }}
              >
                Symbolic Matrix Determinants
                <br />
                <span className="text-5xl">Made Simple</span>
              </motion.h1>
              
              <motion.p
                className="text-xl text-gray-600 max-w-3xl mx-auto mb-10 leading-relaxed"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ duration: 0.6, delay: 0.2 }}
              >
                A powerful Java implementation for computing exact algebraic expressions of
                parameterized tridiagonal matrices with symbolic expansion, simplification,
                and high-precision LU validation.
              </motion.p>

              <motion.div
                className="flex items-center justify-center gap-4 mb-20"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, delay: 0.3 }}
              >
                <Button 
                  size="lg" 
                  className="bg-blue-600 hover:bg-blue-700 px-8"
                  onClick={() => setShowCalculator(true)}
                >
                  Launch Calculator
                </Button>
                <Button size="lg" variant="outline">
                  View Documentation
                </Button>
              </motion.div>

              {/* Visual Matrix Display */}
              <div className="flex items-center justify-center gap-12 mb-8">
                <MatrixVisualization size={4} delay={0.5} />
                <motion.div
                  className="text-4xl text-gray-300 font-light"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ duration: 0.5, delay: 0.8 }}
                >
                  →
                </motion.div>
                <motion.div
                  className="text-center"
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ duration: 0.6, delay: 1 }}
                >
                  <div className="text-sm text-gray-500 mb-2">Closed-Form Result</div>
                  <div className="font-mono text-2xl text-blue-600 bg-blue-50 px-6 py-3 rounded-xl border border-blue-100">
                    det(T) = f(a,b,c)
                  </div>
                </motion.div>
              </div>
            </div>

            {/* Process Flow */}
            <div id="how-it-works" className="mb-24">
              <motion.div
                className="text-center mb-12"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ duration: 0.6, delay: 0.3 }}
              >
                <h2 className="text-3xl mb-3">How It Works</h2>
                <p className="text-gray-600">Four-step computational pipeline</p>
              </motion.div>
              <ProcessFlow />
            </div>

            {/* Code Example */}
            <div className="max-w-3xl mx-auto mb-24">
              <CodeExample />
            </div>

            {/* Features Grid */}
            <div id="features" className="mb-24">
              <motion.div
                className="text-center mb-12"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ duration: 0.6 }}
              >
                <h2 className="text-3xl mb-3">Core Capabilities</h2>
                <p className="text-gray-600">
                  Enterprise-grade symbolic computation with precision validation
                </p>
              </motion.div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                <FeatureCard
                  icon={Sigma}
                  title="Symbolic Expansion"
                  description="Construct exact algebraic expressions from parameterized matrices with full symbolic manipulation capabilities."
                  delay={0.1}
                />
                <FeatureCard
                  icon={Layers}
                  title="Expression Simplification"
                  description="Advanced algorithms reduce complex expressions to elegant closed-form polynomials with optimal complexity."
                  delay={0.2}
                />
                <FeatureCard
                  icon={CheckCircle2}
                  title="LU-Based Validation"
                  description="High-precision numeric verification using LU decomposition ensures correctness of symbolic results."
                  delay={0.3}
                />
                <FeatureCard
                  icon={Network}
                  title="Tridiagonal Matrices"
                  description="Specialized support for tridiagonal and band matrices commonly found in scientific computing."
                  delay={0.4}
                />
                <FeatureCard
                  icon={Calculator}
                  title="Numeric Evaluation"
                  description="Convert symbolic expressions to numeric results with arbitrary precision arithmetic support."
                  delay={0.5}
                />
                <FeatureCard
                  icon={Sparkles}
                  title="Custom Algebra Layer"
                  description="Purpose-built symbolic algebra system designed specifically for matrix determinant computation."
                  delay={0.6}
                />
              </div>
            </div>

            {/* CTA Section */}
            <motion.div
              className="bg-gradient-to-br from-blue-600 to-blue-700 rounded-3xl p-12 text-center text-white"
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.7, delay: 0.3 }}
            >
              <h2 className="text-3xl mb-4">Ready to compute?</h2>
              <p className="text-blue-100 text-lg mb-8 max-w-2xl mx-auto">
                Start computing symbolic determinants with precision and speed. 
                Built for researchers, engineers, and mathematicians.
              </p>
              <div className="flex items-center justify-center gap-4">
                <Button size="lg" variant="secondary" className="bg-white text-blue-600 hover:bg-gray-50">
                  Try Live Demo
                </Button>
                <Button size="lg" variant="outline" className="border-white text-white hover:bg-blue-500">
                  Read the Paper
                </Button>
              </div>
            </motion.div>
          </section>

          {/* Footer */}
          <footer className="border-t border-gray-100 bg-white">
            <div className="max-w-7xl mx-auto px-6 py-8">
              <div className="flex items-center justify-between text-sm text-gray-500">
                <div className="flex items-center gap-2">
                  <Calculator className="w-4 h-4" />
                  <span>SymbolicDet CAS</span>
                </div>
                <div className="flex gap-6">
                  <a href="#" className="hover:text-gray-900 transition-colors">Documentation</a>
                  <a href="#" className="hover:text-gray-900 transition-colors">API Reference</a>
                  <a href="#" className="hover:text-gray-900 transition-colors">GitHub</a>
                </div>
                <div>© 2026 All rights reserved</div>
              </div>
            </div>
          </footer>
        </>
      )}
    </div>
  );
}